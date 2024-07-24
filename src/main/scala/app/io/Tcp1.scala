package app.io

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Tcp.IncomingConnection
import scala.concurrent.Future
import akka.util.ByteString
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Tcp.ServerBinding
import akka.stream.scaladsl.Tcp
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import scala.concurrent.duration._
import akka.util.Timeout
import akka.Done
import scala.concurrent.ExecutionContextExecutor
import akka.stream.javadsl.Sink
import akka.NotUsed

object Tcp1 extends App {

  import akka.stream.scaladsl.Framing

  implicit val system: ActorSystem = ActorSystem("Tcp1")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  implicit val timeout: Timeout = Timeout(3.second)

  val host = "localhost"

  val port = 7777

  class EchoActor extends Actor {
    import EchoActor._
    def receive: Receive = {
      case Command.Ping(bs) =>
        self ! Command.DoReply(bs.utf8String, sender())

      case Command.DoReply(s, replyTo) if s.toLowerCase.contains("exit") =>
        replyTo ! Event.Exit

      case Command.DoReply(s, replyTo) =>
        replyTo ! Event.Pong(s"Pong[${s}]\n")
    }
  }

  object EchoActor {
    def props = Props(new EchoActor)

    sealed trait Command

    object Command {
      final case class Ping(bs: ByteString) extends Command
      final case class DoReply(str: String, replyTo: ActorRef) extends Command
    }

    sealed trait Event

    object Event {
      final case class Pong(str: String) extends Event
      final case object Exit extends Event
    }

  }

  val echoRef: ActorRef = system.actorOf(EchoActor.props, "EchoActor")

  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp(system).bind(host, port).log("Tcp1")

  val done: Future[Done] =
    connections
      .log("Incoming")
      .runForeach { conn =>
        val exitHandler =
          Flow[String].takeWhile(!_.contains("Bye"), inclusive = false)

        val welcome: Source[String, NotUsed] =
          Source.single(
            s"Welcome to: ${conn.localAddress}, you are: ${conn.remoteAddress}\n"
          )

        val echo =
          Flow[ByteString]
            .via(
              Framing.delimiter(
                ByteString("\n"),
                maximumFrameLength = 256,
                allowTruncation = true
              )
            )
            .map(EchoActor.Command.Ping.apply)
            .ask[EchoActor.Event](echoRef)
            .map {

              case EchoActor.Event.Pong(str) => str

              case EchoActor.Event.Exit =>
                // conn.flow.runWith(
                //   Source.single(ByteString("Bye")),   // TODO: not reachable
                //   Sink.head()
                // )
                "Bye\n"
            }
            .merge(welcome)
            .via(exitHandler)
            .map(ByteString(_))

        conn.handleWith(echo)
      }

  done.foreach(_ => println("Done"))

}
