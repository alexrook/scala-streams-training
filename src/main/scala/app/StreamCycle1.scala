package app

import akka.stream.scaladsl._
import akka.stream.OverflowStrategy
import akka.stream.ClosedShape
import akka.actor.ActorSystem
import akka.NotUsed
import akka.stream.Attributes

//https://doc.akka.io/docs/akka/current/stream/stream-graphs.html#graph-cycles-liveness-and-deadlocke
object StreamCycle1 extends App {

  implicit val system: ActorSystem = ActorSystem("StreamCycle")

  val source = Source.fromIterator(() => LazyList.from(0).iterator)

  val graph1: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val merge = b.add(Merge[Int](2))
      val bcast = b.add(Broadcast[Int](2))

      source ~> merge ~> bcast ~> Sink.foreach(println)
      // без использования drop стрим зависнет
      //  merge <~ bcast
      merge <~ Flow[Int].buffer(10, OverflowStrategy.dropHead) <~ bcast
      ClosedShape
    })

  graph1.withAttributes(Attributes.inputBuffer(10, 10)).run()

}
