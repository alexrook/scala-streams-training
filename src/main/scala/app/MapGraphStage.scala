package app

import akka.stream.stage.GraphStage
import akka.stream.FlowShape
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.Attributes
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler

object MapGraphStage extends App {
  class Map[A, B](f: A => B) extends GraphStage[FlowShape[A, B]] {

    val in = Inlet[A]("Map.in")
    val out = Outlet[B]("Map.out")

    override val shape = FlowShape.of(in, out)

    override def createLogic(attr: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = {
              // upstream дал нам элемент,
              // мы берем его (grab)
              // преобразуем(f)
              // и толкаем(push) в downstream
              push(out, f(grab(in)))
            }
          }
        )
        setHandler(
          out,
          new OutHandler {
            override def onPull(): Unit = {
              pull(in) // downstream требует элемент мы требуем его от upstream
            }
          }
        )
      }
  }
}
