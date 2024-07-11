package app

import akka.stream.scaladsl._
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.NotUsed
import akka.Done
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContextExecutor

object ActorInterOp2 extends App {

  implicit val system: ActorSystem = ActorSystem("ActorInterOp2")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  class WithBackpressureActor extends Actor {
    import WithBackpressureActor._
    override def receive: Receive = {

      case Command.Init =>
        sender() ! Answer.Ack

      case Command.DoWork(v) =>
        sender() ! Answer.Reply(s"Reply[$v]")

      case Command.Complete =>
        println("The stream completed")
        context.stop(self)

      case Command.Failure(ex) =>
        ex.printStackTrace()
        context.stop(self)

    }
  }

  object WithBackpressureActor {
    sealed trait Command

    object Command {
      final case object Init extends Command
      final case object Complete extends Command
      final case class DoWork(v: String) extends Command
      final case class Failure(ex: Throwable) extends Command
    }

    sealed trait Answer

    object Answer {
      final case object Ack extends Answer
      final case class Reply(s: String) extends Answer
    }
  }

  val ref = system.actorOf(Props(new WithBackpressureActor))

  val sink: Sink[Any, NotUsed] =
    Sink.actorRefWithBackpressure(
      ref,
      onInitMessage = WithBackpressureActor.Command.Init,
      ackMessage = WithBackpressureActor.Answer.Ack,
      onCompleteMessage = WithBackpressureActor.Command.Complete,
      onFailureMessage =
        (ex: Throwable) => ref ! WithBackpressureActor.Command.Failure(ex = ex)
    )

  val ret: Future[Done] =
    Source(1 to 10)
      .map { i =>
        WithBackpressureActor.Command.DoWork(i.toString())
      }
      .alsoTo(sink)
      .runForeach(println)

  ret.onComplete {
    case Failure(exception) =>
      exception.printStackTrace()
      sys.exit(-1)
    case Success(_) =>
      system.terminate().foreach(_ => println("Done"))
  }

}
