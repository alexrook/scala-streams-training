package app

import akka.actor._

import akka.stream._
import akka.stream.scaladsl._
import akka.NotUsed

object ActorRefDSL extends App {
  import SumActor._

  implicit val system: ActorSystem = ActorSystem("ActorRefDSL")

  val sumActor = system.actorOf(SumActor.props, "SumActor")

  val sink = Sink.actorRef(
    ref = sumActor,
    onCompleteMessage = Command.StreamComplete
  )

  

  val source = Source.fromIterator(() => List.fill(12)("A").iterator)

  val ret = source.viaMat(???)(???)
  .runWith(sink)

  system.terminate()

}

class SumActor extends Actor {
  import SumActor._

  override def receive: Receive = { case Command.StreamComplete =>
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
