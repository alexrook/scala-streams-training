package app

import akka.stream.scaladsl.Source
import akka.actor.ActorSystem
import java.io.IOException
import akka.Done
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContextExecutor

/** recover позволяет выбросить _последний_ элемент в поток и завершить работу
  */
object StreamErrorHandling1 extends App {

  implicit val system: ActorSystem = ActorSystem("StreamErrorHandling")

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val random = scala.util.Random.nextInt(10)
  val ret: Future[Done] = Source(0 to 10)
    .map(i =>
      if (random == i)
        throw new IOException(s"Something went wrong with i[$i]")
      else s"All ok[$i]"
    )
    .recover { case e: IOException =>
      s"Catched[${e.getMessage()}]"
    }
    .runForeach(println)

  ret.onComplete {
    case Failure(ex) =>
      println("\nFailure case:")
      ex.printStackTrace()
      sys.exit(-1)
    case Success(value) =>
      system.terminate().foreach(_ => println("Done"))
  }
}
