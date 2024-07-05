package app

import akka.stream.scaladsl._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContextExecutor
import akka.Done
import scala.concurrent._

import scala.util._
import akka.NotUsed

object GroupingDSL extends App {

  implicit val system: ActorSystem = ActorSystem("GroupingDSL")

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val src = Source.fromIterator(() => List.fill(12)("A").iterator)

  /** Chunk up this stream into groups of elements received within a time
    * window, or limited by the given number of elements, whatever happens
    * first.
    *
    * Empty groups will not be emitted if no elements are received from
    * upstream. The last group before end-of-stream will contain the buffered
    * elements since the previously emitted group.
    */
  val grouped = src.groupedWithin(3, 3.millis)

  val ret1: Future[Done] = grouped
    .runWith(Sink.foreach(println))

  val ret2: Future[Done] = grouped
    .mapAsync(4) { seq =>
      Future.successful(seq)
    }
    .runWith(Sink.foreach(println))

  (for {
    r1 <- ret1
    r2 <- ret2
  } yield {
    ()
  }).onComplete {
    case Failure(ex) =>
      ex.printStackTrace()
      sys.exit(-1)
    case Success(_) =>
      system.terminate()
  }

}
