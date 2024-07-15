package app

import akka.actor._

import akka.stream._
import akka.stream.scaladsl._
import akka.NotUsed
import scala.concurrent.ExecutionContextExecutor
import app.ActorInterOps4.{streamSize => streamSize}

/** Actor Sink without backpressure
  */
object ActorInterOps4 extends App {
  import SumActor._

  implicit val system: ActorSystem = ActorSystem("ActorRefDSL")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val streamSize = 12

  val sumActor = system.actorOf(SumActor.props, "SumActor")

  val sink = Sink.actorRef(
    ref = sumActor,
    onCompleteMessage = Command.StreamComplete
  )

  val source = Source.fromIterator(() => List.fill(streamSize)("A").iterator)

  val ret = source.alsoTo(sink).runForeach(println)

  ret.onComplete { _ =>
    system.terminate()
  }

}

class SumActor extends Actor {
  import SumActor._

  var allLength = 0L

  override def receive: Receive = {

    case s: String =>
      allLength += s.length().toLong

    case Command.StreamComplete =>
      println(s"Total length[$allLength]")
      assert(allLength == streamSize)
      println("The Stream has been completed")
  }

}

object SumActor {
  def props: Props = Props(new SumActor)

  sealed trait Command

  object Command {
    object StreamComplete extends Command
  }

}
