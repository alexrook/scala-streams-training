package app

import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.Attributes
import akka.dispatch.MessageDispatcher
import akka.NotUsed
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object FutureInterOp1 extends App {
  val cfgDispRaw =
    s"""
        blocking-dispatcher {
             executor = "thread-pool-executor"
                thread-pool-executor {
                core-pool-size-min    = 10
                core-pool-size-max    = 10
                }
        }
        """

  val cfg: Config =
    ConfigFactory.load().withFallback(ConfigFactory.parseString(cfgDispRaw))

  implicit val system: ActorSystem = ActorSystem("FutureInterOp1", cfg)

  class SometimesSlowService(implicit ec: ExecutionContext) {

    private val runningCount = new AtomicInteger

    def convert(s: String): Future[String] = {
      println(s"running: $s (${runningCount.incrementAndGet()})")
      Future {

        if (s.headOption.exists(_.isLower)) {
          // Elements starting with a lower case char are simulated
          // to take longer time to process.
          Thread.sleep(500)
        } else {
          Thread.sleep(20)
        }
        println(s"completed: $s (${runningCount.decrementAndGet()})")

        s.toUpperCase

      }
    }
  }

  implicit val blockEC: MessageDispatcher =
    system.dispatchers.lookup("blocking-dispatcher")

  val slowService = new SometimesSlowService

  Source(List("a", "B", "C", "D", "e", "F", "g", "H", "i", "J"))
    .map(elem => { println(s"before: $elem"); elem })
    .mapAsync(4)(slowService.convert)
    .to(Sink.foreach(elem => println(s"after: $elem")))
    .withAttributes(Attributes.inputBuffer(initial = 4, max = 4))
    .run()

  //system.terminate().foreach(_ => println("Done"))

}
