package app

import akka.util.Timeout
import scala.concurrent.duration._
import akka.stream.scaladsl._
import akka.NotUsed
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.ExecutionContextExecutor
/**
  * запрос актора для каждого элемента стримаы
  */
object ActorInterOp1 extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = { case s: String => sender ! "Reply[s]" }
  }

  implicit val system: ActorSystem = ActorSystem("ActorInterOp")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  implicit val askTimeout: Timeout = 5.seconds
  val words: Source[String, NotUsed] =
    Source(List("hello", "hi", "akka"))

  val ref = system.actorOf(Props(new SimpleActor))

  

  words
    .ask[String](parallelism = 5)(ref)
    // continue processing of the replies from the actor
    .map(_.toLowerCase)
    .runWith(Sink.foreach(println))
    .andThen { _ =>
      system.terminate()
    }
    .foreach { _ =>
      println("Done")
    }

}
