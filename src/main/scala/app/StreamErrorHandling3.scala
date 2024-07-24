package app

import akka.stream.Supervision
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.ActorAttributes
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure
import scala.util.Success

/** Добавление аттрибутов к потоку для установки поведения в случае ошибки
  */
object StreamErrorHandling3 extends App {

  implicit val system: ActorSystem = ActorSystem("StreamErrorHandling3")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val decider: Supervision.Decider = {
    case _: ArithmeticException => Supervision.Resume
    case _                      => Supervision.Stop
  }

  val source = Source(0 to 5).map(100 / _)

  // division by zero here
  val runnableGraph =
    source.toMat(Sink.fold(0)(_ + _))(Keep.right)

  val withCustomSupervision =
    runnableGraph.withAttributes(ActorAttributes.supervisionStrategy(decider))

  withCustomSupervision.run().onComplete {
    case Failure(ex) =>
      println("An error occurred:")
      ex.printStackTrace()
      sys.exit(-1)
    case Success(value) =>
      system.terminate().foreach(_ => println(s"Done[$value]"))
  }

}
