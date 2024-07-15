package app

import akka.stream.scaladsl._
import akka.stream.scaladsl.GraphDSL
import akka.actor.ActorSystem
import akka.stream.ClosedShape

object StreamCycle2 extends App {

  implicit val system: ActorSystem = ActorSystem("StreamCycle")

  val source = Source.fromIterator(() => LazyList.from(0).iterator)

  val graph =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val zip = b.add(ZipWith((left: Int, right: Int) => left))
      val bcast = b.add(Broadcast[Int](2))

      // что бы запустить первый zip нужно два значения
      // но их нет пока первая пара не попадет в bcast
      // поэтому используется первое значение как start
      val concat = b.add(Concat[Int]())
      val start = Source.single(0)

      source ~> zip.in0
      zip.out.map { s => println(s); s } ~> bcast ~> Sink.ignore
      zip.in1 <~ concat <~ start
      concat <~ bcast
      ClosedShape
    })

  graph.run()

}
