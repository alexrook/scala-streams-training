package app

import akka.stream.scaladsl._
import akka.actor.ActorSystem
import akka.Done
import akka.NotUsed
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Keep
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.Cancellable
import akka.stream.scaladsl.GraphDSL
import akka.stream.ClosedShape
import app.JediValues.promise
import akka.stream.BoundedSourceQueue

object Boot extends App {

  implicit val system: akka.actor.ActorSystem = ActorSystem(
    "AkkaStreamsSystem"
  )

  implicit val ec: ExecutionContextExecutor = system.dispatcher

//   val source = Source(1 to 10)
//   val sink1: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

//   val sink2: Sink[String, Future[Done]] = Sink.foreach[String](println)

//   val flow = Flow[Int].map { i =>
//     i.toString()
//   }

// // materialize the flow, getting the Sink's materialized value
//   val sum: Future[Int] = source.runWith(sink1)

//   // val r: (NotUsed, Future[Done]) = flow.runWith(source, sink2)

//   // connect the Source to the Sink, obtaining a RunnableGraph
//   val sink = Sink.fold[Int, Int](0)(_ + _)
//   val runnable: RunnableGraph[Future[Int]] =
//     Source(1 to 10).toMat(sink)(Keep.right)

// // get the materialized value of the sink
//   val sum1: Future[Int] = runnable.run()
//   val sum2: Future[Int] = runnable.run()
// // sum1 and sum2 are different Futures!

//   val s1 = Await.result(sum1, 1.second)
//   val s2 = Await.result(sum2, 1.second)

  val source = Source.maybe[String]

  val (promise: Promise[Option[String]], r: Future[Done]) =
    source.toMat(Sink.foreach(println))(Keep.both).run()

  promise.success(Some("Hi"))

  Await.ready(r, 10.second)

  val source2: Source[String, BoundedSourceQueue[String]] =
    Source.queue[String](1000)

  val (queue, r2) =
    source2.toMat(Sink.foreach(println))(Keep.both).run()

  for (x <- 0 to 5) {
    queue.offer(s"asas[$x]")
  }

  queue.complete()

  Await.ready(r2, 10.second)

  val prices = Source(List(100, 101, 99, 103))
  val quantity = Source(List(1, 3))

  val r3: Future[Done] = prices
    .mergeLatest(quantity)
    .map { case price :: quantity :: Nil =>
      price -> quantity
    }
    .runForeach(println)

  Await.ready(r3, 10.second)

  system.registerOnTermination(
    println("Done")
  )

  system.terminate()

}
