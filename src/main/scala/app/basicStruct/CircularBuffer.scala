package app.basicStruct

import scala.collection.mutable.ArrayDeque

class CircularBuffer[T](val capacity: Int) {
  private val buffer = new ArrayDeque[T]()

  def add(element: T): Unit = {
    if (buffer.size >= capacity) {
      buffer.removeHead()
    }
    buffer.append(element)
  }

  def get: Seq[T] = buffer.toSeq
}

object CircularBufferTest extends App {
  val cb = new CircularBuffer[Int](3)

  cb.add(1)
  cb.add(2)
  cb.add(3)
  println(cb.get) // Output: ArrayBuffer(1, 2, 3)

  cb.add(4)
  println(cb.get) // Output: ArrayBuffer(2, 3, 4)

  cb.add(5)
  println(cb.get) // Output: ArrayBuffer(3, 4, 5)
}
