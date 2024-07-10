package app

import akka.stream._

object ReusableComponents extends App {

  // A shape represents the input and output ports of a reusable
// processing module
  case class PriorityWorkerPoolShape[In, Out](
      jobsIn: Inlet[In],
      priorityJobsIn: Inlet[In],
      resultsOut: Outlet[Out]
  ) extends Shape {

    // It is important to provide the list of all input and output
    // ports with a stable order. Duplicates are not allowed.
    override val inlets: Seq[Inlet[_]] =
      jobsIn :: priorityJobsIn :: Nil
    override val outlets: Seq[Outlet[_]] =
      resultsOut :: Nil

    // A Shape must be able to create a copy of itself. Basically
    // it means a new instance with copies of the ports
    override def deepCopy() =
      PriorityWorkerPoolShape(
        jobsIn.carbonCopy(),
        priorityJobsIn.carbonCopy(),
        resultsOut.carbonCopy()
      )

  }

  import FanInShape.{Init, Name}

  class PriorityWorkerPoolShape2[In, Out](
      _init: Init[Out] = Name("PriorityWorkerPool")
  ) extends FanInShape[Out](_init) {
    protected override def construct(i: Init[Out]) =
      new PriorityWorkerPoolShape2(i)

    val jobsIn = newInlet[In]("jobsIn")
    val priorityJobsIn = newInlet[In]("priorityJobsIn")
    // Outlet[Out] with name "out" is automatically created
  }

  val c =
    new PriorityWorkerPoolShape2[Int, String](_init = Name[String]("Test"))

}

object TestA extends App {
  class StreamProcessor {
    def process(inlet: Inlet[Int]): Unit = {
      val copy = inlet.carbonCopy()
    //   println(s"Original: ${inlet.s}, Mapped To: ${inlet.mappedTo.s}")
    //   println(s"Copy: ${copy.s}, Mapped To: ${copy.mappedTo.s}")
    }
  }

  val inlet = Inlet[Int]("source")
  val processor = new StreamProcessor
  processor.process(inlet)

}
