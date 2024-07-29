package app

import akka.stream.scaladsl._

import scala.concurrent.duration._
import akka.stream.KillSwitches
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContextExecutor

object KillSwitch1 extends App {

  implicit val system: akka.actor.ActorSystem = ActorSystem("KillSwitch1")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val countingSrc = Source.tick(0.second, 1.second, "A")

  val sink = Sink.foreach(println)

  val (killSwitch, ret) =
    countingSrc
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(sink)(Keep.both)
      .run()

  ret.onComplete {
    case Failure(ex) =>
      ex.printStackTrace()
      sys.exit(-1)
    case Success(v) =>
      system
        .terminate()
        .foreach(_ => println(s"the program completed with[$v]"))
  }
  
  Thread.sleep(3000)

  killSwitch.shutdown()

}
