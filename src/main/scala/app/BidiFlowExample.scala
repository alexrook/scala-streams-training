package app

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, BidiShape}
import akka.NotUsed

object BidiFlowExample extends App {
  implicit val system: ActorSystem = ActorSystem("BidiFlowExample")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Функция шифрования (например, простое реверсирование строки)
  def encrypt(s: String): String = s.reverse


  // Функция дешифрования (реверсирование строки обратно)
  def decrypt(s: String): String = s.reverse

  // Создание BidiFlow
  val bidiFlow: BidiFlow[String, String, String, String, NotUsed] =
    BidiFlow.fromFunctions(encrypt, identity[String])

  // Входные данные для прямого и обратного направлений
  val sourceEncrypt = Source(List("hello", "world"))
  val sourceDecrypt = Source(List("olleh", "dlrow"))

  
  // Композиция потоков с использованием только BidiFlow
  val encrypted = sourceEncrypt.via(bidiFlow.join(Flow[String]))
  val decrypted = sourceDecrypt.via(bidiFlow.join(Flow[String]))

  // Печать результатов
  encrypted.runWith(Sink.foreach(println)) // Вывод: olleh, dlrow
  
}
