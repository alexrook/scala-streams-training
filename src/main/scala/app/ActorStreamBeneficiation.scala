package app

import akka.stream.stage._
import akka.stream._
import akka.actor._
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.util.Success
import scala.util.Failure
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import akka.NotUsed
import akka.Done
import scala.concurrent.Future

object ActorStreamBeneficiation extends App {

  implicit val system: ActorSystem = ActorSystem("ActorStreamBeneficiation")
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  class DatabaseActor extends Actor {

    import DatabaseActor._

    val database: String => Option[Int] =
      (Map(
        "A" -> 1,
        "B" -> 2,
        "C" -> 3,
        "D" -> 1
      ).orElse {
        case s: String if s.length < 3 => s.length
      }: PartialFunction[String, Int]).lift

    override def receive: Receive = { case Command.Enrich(key) =>
      sender() ! Answer.Enriched(key, database(key))
    }
  }

  object DatabaseActor {
    sealed trait Command

    object Command {
      final case class Enrich(key: String)
    }

    sealed trait Answer

    object Answer {
      final case class Enriched(key: String, value: Option[Int])
    }
  }

  // will close upstream in all materializations of the graph stage instance
// when the future completes
  class FromActorStreamBenefication(dbActor: ActorRef)
      extends GraphStage[FlowShape[String, (String, Option[Int])]] {

    val in = Inlet[String]("FromActorStreamBenefication.in")
    val out = Outlet[(String, Option[Int])]("FromActorStreamBenefication.out")

    val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        var callback: AsyncCallback[(String, Option[Int])] = _

        var finished: Boolean = false

        override def preStart(): Unit = {
          callback = getAsyncCallback[(String, Option[Int])] {
            case (el @ (key: String, value: Option[Int])) =>
              push(out, el)
              if (finished) {
                completeStage()
              }
          }
        }

        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = {

              val key: String = grab(in)

              dbActor
                .ask(DatabaseActor.Command.Enrich(key))
                .mapTo[DatabaseActor.Answer.Enriched]
                .map { case DatabaseActor.Answer.Enriched(key, value) =>
                  key -> value
                }
                .onComplete {
                  case Success(v) =>
                    callback.invoke(v)
                  case Failure(ex) =>
                    throw ex
                }

            }

            override def onUpstreamFinish() = {
              finished = true
            }
          }
        )
        setHandler(
          out,
          new OutHandler {
            override def onPull(): Unit = { pull(in) }
          }
        )
      }
  }

  val dbActor: ActorRef = system.actorOf(Props(new DatabaseActor))

  val flow: Flow[String, (String, Option[Int]), NotUsed] =
    Flow.fromGraph(new FromActorStreamBenefication(dbActor))

  val stream: Future[Done] =
    Source(List("A", "B", "C", "a", "ddd", "aasedwedwed"))
      .via(flow)
      .runForeach(println)

  val stream2 =
    Source(List.fill(100)(scala.util.Random.nextString(2)))
      .via(flow)
      .runForeach(println)

  (for {
    _ <- stream
    _ <- stream2
  } yield {
    Done
  })
    .onComplete {
      case Failure(exception) =>
        exception.printStackTrace()
      case Success(_) =>
        system.terminate()
        println("Done")
    }

}
