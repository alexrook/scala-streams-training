package app

import akka.stream.scaladsl._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.compat.Future
import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure
import scala.util.Success

/** from
  * https://doc.akka.io/docs/akka/current/stream/stream-dynamic.html#dynamic-fan-in-and-fan-out-with-mergehub-broadcasthub-and-partitionhub
  * A MergeHub allows to implement a dynamic fan-in junction point in a graph
  * where elements coming from different producers are emitted in a
  * First-Comes-First-Served fashion. If the consumer cannot keep up then all of
  * the producers are backpressured. The hub itself comes as a Source to which
  * the single consumer can be attached. It is not possible to attach any
  * producers until this Source has been materialized (started). This is ensured
  * by the fact that we only get the corresponding Sink as a materialized value.
  */
object DynamicSource extends App {

  implicit val system: ActorSystem = ActorSystem("DynamicSource")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  // A simple consumer that will print to the console for now
  val consumer = Sink.foreach(println)

// Attach a MergeHub Source to the consumer. This will materialize to a
// corresponding Sink.
  val runnableGraph: RunnableGraph[Sink[String, NotUsed]] =
    MergeHub
      .source[String](perProducerBufferSize = 16)
      .map { s => s -> s.length() }
      .to(consumer)

// By running/materializing the consumer we get back a Sink, and hence
// now have access to feed elements into it. This Sink can be materialized
// any number of times, and every element that enters the Sink will
// be consumed by our consumer.
  val toConsumer: Sink[String, NotUsed] = runnableGraph.run()

// Feeding two independent sources into the hub.
  Source.single("Hello!").runWith(toConsumer)
  Source.single("Hub!").runWith(toConsumer)

  Future
    .sequence {
      List.fill(10)(scala.util.Random.nextLong(1000)).map { l =>
        Future {
          Thread.sleep(l)
          Source.single(s"From[$l]").runWith(toConsumer)
        }
      }
    }
    .onComplete {
      case Failure(ex) =>
        throw ex
        sys.exit(-1)
      case Success(_) =>
        system.terminate()
        println("The program Done")
    }

}
