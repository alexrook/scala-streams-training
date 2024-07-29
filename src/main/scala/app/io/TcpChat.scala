package app.io

import java.nio.charset.StandardCharsets.UTF_8

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source, Tcp}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import scala.io.StdIn
import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl.GraphDSL
import akka.stream.SinkShape
import akka.stream.scaladsl.Merge
import akka.NotUsed
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContextExecutor

/** example from
  * https://stackoverflow.com/questions/36242183/how-to-implement-a-simple-tcp-protocol-using-akka-streams
  */

object Shared {

  type Message = String

  val host = "localhost"
  val port = 7777

  def incoming(mark: String): Flow[ByteString, Message, _] =
    Flow.fromFunction { bs: ByteString =>
      s"\n<<[${bs.utf8String}]<<$mark\n"

    }
  val outgoing: Flow[Message, ByteString, _] =
    Flow.fromFunction(ByteString.apply)

  def protocol(mark: String) = BidiFlow.fromFlows(incoming(mark), outgoing)

  // the problem is that Akka Stream does operator fusion.
  // This means that the complete flow handling runs on a single actor.
  // When it blocks for reading your messages it cannot print out anything.
  // The solution would be to add an async boundary, after your source
  def chattingSource(s: String, initial: String): Source[Message, _] =
    Source
      .single(initial)
      .merge {
        Source.unfoldResource( // unfoldResource deals with blocking non-streamed things
          create = () => Iterator.continually(StdIn.readLine(s"[$s]>")),
          read = (iter: Iterator[String]) => Some(iter.next),
          close = (iter: Iterator[String]) => ()
        )
      } // https://github.com/johanandren/akka-streams-tcp-chat/blob/f0209c5f07bae9bcf950adc3220cb0cdd34da93b/src/main/scala/akkastreamchat/Client.scala#L48C28-L48C85

  def outSink(w: String): Sink[String, NotUsed] =
    Sink.fromGraph {
      GraphDSL.create[SinkShape[String]]() {
        implicit builder: GraphDSL.Builder[NotUsed] =>
          import GraphDSL.Implicits._

          val welcomeSource = Source.single(w)

          val merge = builder.add(Merge[String](2))

          welcomeSource ~> merge.in(0)
          merge.out ~> Sink.foreach(println)

          SinkShape[String](merge.in(1))
      }
    }

}

object Tcp2Server extends App {

  import Shared._

  implicit val system: ActorSystem =
    ActorSystem("akka-stream-tcp-server")

  /** Resulting Flow:
    * {{{
    *       +--------------------------------+
    *       | Source[IncomingConnection,...] |
    *       +--------------------------------+
    *             ↓
    *          +------+    join        +----------+
    * сеть ~>  |      |~> ByteString ~>| incoming |~> Message ~> outSink
    *          | flow |                |   bidi   |
    * сеть <~  |      |<~ ByteString <~| outgoing |<~ Message <~ Server prompt
    *          +------+                +----------+
    * }}}
    */

  Tcp()
    .bind(host, port) // server binding
    .runForeach { conn =>
      conn.flow
        .join(protocol(s"fromClient[${conn.remoteAddress}]"))
        .runWith(
          chattingSource("S", "Hello Client"),
          outSink("Here is the Server chat")
        )
    }

}

object Tcp2Client extends App {

  import Shared._

  implicit val system: ActorSystem =
    ActorSystem("akka-stream-tcp-client")

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val connection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp() // client binding
      .outgoingConnection(host, port)

  connection
    .join(protocol(s"fromServer[$port]"))
    .runWith(chattingSource("C", "Hello Server"), outSink("Here is the Client chat"))

}
