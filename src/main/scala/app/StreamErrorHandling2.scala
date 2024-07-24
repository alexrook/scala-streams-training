package app

import akka.stream.scaladsl.Source
import akka.actor.ActorSystem
import akka.Done
import scala.concurrent.Future
import akka.NotUsed
import scala.concurrent.Await
import scala.concurrent.duration._

object StreamErrorHandling2 extends App {

  implicit val system: ActorSystem = ActorSystem("StreamErrorHandling2")

  val planB: Source[String, NotUsed] = Source(
    List("five", "six", "seven", "eight")
  )

  val ret: Future[Done] =
    Source(0 to 10)
      .map(n =>
        if (n < 5) n.toString
        else throw new RuntimeException("Boom!")
      )
      .recoverWithRetries(
        attempts = 1,
        { case _: RuntimeException =>
          planB
        }
      )
      .runForeach(println)

  Await.ready(ret, 10.seconds)

  system.terminate()
  println("Done")
}
