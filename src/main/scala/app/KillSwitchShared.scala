package app

import scala.concurrent.duration._

import akka.stream.scaladsl._
import akka.stream.KillSwitches
import akka.actor.ActorSystem
import akka.stream.DelayOverflowStrategy
import scala.concurrent.Await
import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure
import scala.util.Success
import akka.Done
import scala.concurrent.Future
import akka.stream.SharedKillSwitch

object KillSwitchShared extends App {

  implicit val system: ActorSystem = ActorSystem("KillSwitchShared")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val src =
    Source.tick(0.second, 1.second, "A")

  def snk(mark: String) =
    Sink.foreach { s: String =>
      println(s"$mark[$s]")
    }

  val sharedKillSwitch: SharedKillSwitch =
    KillSwitches.shared("my-kill-switch")

  val ret: Future[Done] =
    src
      .via(sharedKillSwitch.flow)
      .runWith(snk("First"))

  val ret2: Future[Done] =
    src
      .delay(1.second, DelayOverflowStrategy.backpressure)
      .via(sharedKillSwitch.flow)
      .runWith(snk("Second"))

  ret.flatMap(_ => ret2).onComplete {

    case Failure(exception) =>
      exception.printStackTrace()
      sys.exit(-1)

    case Success(v) =>
      system.terminate().foreach(_ => println(s"Terminated[$v]"))
  }

  Thread.sleep(4000)
  sharedKillSwitch.shutdown()

}
