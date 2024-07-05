package app

import akka.stream.scaladsl._
import scala.concurrent.Future
import akka.stream.ClosedShape
import akka.actor._
import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure
import scala.util.Success

object GraphDSLBroadcast extends App {

  implicit val system: ActorSystem = ActorSystem("GraphDSLBroadcast")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val sinks =
    Seq("a", "b", "c")
      .map(prefix =>
        Flow[String]
          .filter(str => str.startsWith(prefix))
          .toMat(Sink.head[String])(Keep.right)
      )

  val g: RunnableGraph[Seq[Future[String]]] =
    RunnableGraph.fromGraph(GraphDSL.create(sinks) { implicit b => sinkList =>
      import GraphDSL.Implicits._
      val broadcast = b.add(Broadcast[String](sinkList.size))
    
    

      Source(List("ax", "bx", "cx")) ~> broadcast
      sinkList.foreach(sink => broadcast ~> sink)

      ClosedShape
    })

  val matList: Seq[Future[String]] = g.run()

  Future.sequence(matList).onComplete {
    case Failure(ex) =>
      throw ex

    case Success(values) =>
      values.foreach(println)
      system.terminate()
      println("The End")

  }
}
