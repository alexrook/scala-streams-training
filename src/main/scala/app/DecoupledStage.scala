package app

import akka.stream._
import akka.stream.stage._
import scala.collection.mutable.{Queue => MutQueue}

object DecoupledStage extends App {

  class TwoBuffer[A] extends GraphStage[FlowShape[A, A]] {

    val in = Inlet[A]("TwoBuffer.in")
    val out = Outlet[A]("TwoBuffer.out")

    val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        val buffer = MutQueue[A]()

        def bufferFull = buffer.size == 2

        var downstreamWaiting = false

        override def preStart(): Unit = {
          // a detached stage needs to start upstream demand
          // itself as it is not triggered by downstream demand
          pull(in)
        }

        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = {
              val elem = grab(in)
              buffer.enqueue(elem)
              if (downstreamWaiting) {
                downstreamWaiting = false
                //помещение и запрос элемента из очереди позволяет сохранить порядок сообщений
                val bufferedElem = buffer.dequeue()
                push(out, bufferedElem)
              }
              if (!bufferFull) {
                pull(in)
              }
            }

            override def onUpstreamFinish(): Unit = {
              if (buffer.nonEmpty) {
                // emit the rest if possible
                emitMultiple(out, buffer.toIterator)
              }
              completeStage()
            }
          }
        )

        setHandler(
          out,
          new OutHandler {
            override def onPull(): Unit = { // downstream требует элемент
              if (buffer.isEmpty) {
                downstreamWaiting = true // если буфер пуст выставляем флаг
              } else {
                val elem = buffer.dequeue()
                push(out, elem) // или толкаем элемень
              }
              if ( // если буфер не пустой и еще небыло запроса к in

                !bufferFull &&
                !hasBeenPulled(in) // from port lifecycle
              ) {
                pull(in) // делаем запрос к upstream
              }
            }
          }
        )
      }

  }

}
