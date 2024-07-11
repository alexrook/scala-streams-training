package app

import akka.stream.scaladsl._
import akka.stream.OverflowStrategy
import akka.stream.CompletionStrategy
import java.io.IOException
import akka.actor.ActorRef
import akka.actor.ActorSystem
import scala.util._

import scala.concurrent.ExecutionContextExecutor
import akka.Done
import scala.concurrent.Future

/** based on
  * https://doc.akka.io/docs/akka/current/stream/actor-interop.html#source-actorref
  */
object ActorInterOp3 extends App {

  implicit val system: ActorSystem = ActorSystem("ActorInterOp3")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val bufferSize = 10

  // создает minimal actor по переданным функциям
  // этот актор передает сообщения, которые ему пересланы, далее в стрим
  val sourceRef: Source[Int, ActorRef] =
    Source
      .actorRef[Int](
        completionMatcher = {
          case "Complete" => // ловит сигнал о завершении потока
            println("A completion signal received")
            CompletionStrategy.draining
        }: PartialFunction[Any, CompletionStrategy],
        failureMatcher = { case "Break" => // ловит сигнал об ошибке
          new IOException("Will fail")
        }: PartialFunction[Any, Throwable],
        bufferSize = bufferSize,
        overflowStrategy =
          OverflowStrategy.dropTail // note: backpressure is not supported
      )

  val (ref: ActorRef, future: Future[Done]) =
    sourceRef.map(_ * 2).toMat(Sink.foreach(println))(Keep.both).run()

  ref ! 1
  ref ! 10
  ref ! 42
  ref ! 112
  ref ! "Complete"

  future.onComplete {
    case Success(_) =>
      system.terminate.foreach(_ => println("Done"))
    case Failure(exception) =>
      exception.printStackTrace()
      sys.exit(-1)
  }

}
