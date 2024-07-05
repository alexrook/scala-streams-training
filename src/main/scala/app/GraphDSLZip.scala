package app

import akka.stream.scaladsl._
import akka.stream.UniformFanInShape
import akka.stream.ClosedShape
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorSystem

object GraphDSLZip extends App {
  implicit val system: ActorSystem = ActorSystem("GraphDSLZip")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  /**   - n0 🡢 zip1.in0 🡢 🡦 //результат в zip2.in0
    *   - n1 🡢 zip1.in1 🡢 🡢 zip2.in0 //результат в zip2.in0
    *   - n3 🡢 zip2.in1 🡢 out
    *
    * The same as
    * ```
    *  val zip3 = ZipWith[Int, Int, Int, Int] { case (x1, x2, x3) =>
    *     List(x1, x2, x3).max
    * }
    * ```
    */

  val pickMaxOfThree =
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      // из двух чисел большее
      val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
      // из двух чисел большее
      val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))
      zip1.out ~> zip2.in0

      UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
    }

  val resultSink = Sink.head[Int]

  val g = RunnableGraph.fromGraph(GraphDSL.createGraph(resultSink) {
    implicit b => sink =>
      import GraphDSL.Implicits._

      // importing the partial graph will return its shape (inlets & outlets)
      val pm3 = b.add(pickMaxOfThree)

      Source.single(1) ~> pm3.in(0)
      Source.single(2) ~> pm3.in(1)
      Source.single(3) ~> pm3.in(2)
      pm3.out ~> sink.in
      ClosedShape
  })

  val max: Future[Int] = g.run()

  
  Await.result(max, 300.millis)
}
