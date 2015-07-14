package com.example

import akka.actor._
import akka.routing.RoundRobinRouter
import scala.concurrent.duration._
import scala.concurrent.duration.Duration

/**
 * Created by hoang on 11/07/15.
 *
 * This class is to calculate pi number based on the algorithm: pi/4 = 1/1 - 1/3 + 1/5 - 1/7 + ....
 * We will use akka to devide this series into chunks and calculate them in parallel
 */
object Pi extends App{

  calculate(5, 10000, 10000)

  /**
   * Declare a message trait
   */
  sealed trait PiMessage

  /**
   * This message is to signal master to send calculation to action
   */
  case object Calculate extends PiMessage

  /**
   * This message is to send to worker to calculate a chunk of numbers
   *
   * @param start the started item in the series to calculate
   * @param noOfElements the number of items in the calculating chunk
   */
  case class Work(start:Int, noOfElements:Int) extends PiMessage

  /**
   * Message contain result of one chunk
   *
   * @param value result of a chunk
   */
  case class Result(value:Double) extends PiMessage

  /**
   * Final message contain the result of pi with duration to calculate it
   *
   * @param pi pi result
   * @param duration time to calculate pi value
   */
  case class PiApproximation(pi:Double, duration:Duration)

  /**
   * This worker actor receive calculate tasks from master actors and send back the result in CalculatePiFor message
   */
  class Worker extends Actor{
    def calculatePiFor(start:Int, noOfElements:Int):Double = (for (i <- start until (start + noOfElements)) yield 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)).sum

    /**
     * Receive message that listen to incoming message
     *
     * @return return CalculatePiFor message
     */
    override def receive: Receive = {
      case Work(start, noOfElements) => {
        sender ! Result(calculatePiFor(start, noOfElements))
      }
    }
  }

  /**
   * This master actor will send calculate task to worker actors
   *
   * @param noOfWorkers number of workers (parallel processes)
   * @param noOfElements number of chunks that we want to use to estimate Pi
   * @param noOfMessages length of each chunk
   * @param listener system actor that will listen to the final result from master actor
   */
  class Master(noOfWorkers: Int, noOfMessages: Int, noOfElements: Int, listener: ActorRef) extends Actor {

    var pi = 0D
    var noOfResults = 0
    val workerRouter = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(noOfWorkers)),name="workerRouter")
    val start: Long = System.currentTimeMillis

    override def receive: Actor.Receive = {
      case Calculate => for (i <- 0 to noOfMessages) workerRouter ! Work(i*noOfElements,noOfElements)
      case Result(value) => {
        pi += value
        noOfResults += 1
        if(noOfResults == noOfMessages){
          listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
          context.stop(self)
        }
      }
    }
  }

  /**
   * This actor listens to master to receive the final result from it.
   */
  class Listener extends Actor{
    override def receive: Actor.Receive = {
      case PiApproximation(pi,duration) => {
        println(s"\n\nPi approximation: ${pi} in ${duration} mseconds")
        context.system.shutdown()
      }
    }
  }

  /**
   * This is started method of the application
   *
   * @param noOfWorkers number of workers (parallel processes)
   * @param noOfElements number of chunks that we want to use to estimate Pi
   * @param noOfMessages length of each chunk
   */
  def calculate(noOfWorkers:Int, noOfElements:Int, noOfMessages:Int) {
    val system = ActorSystem("PiSystem")
    val listener = system.actorOf(Props[Listener], name="listener")
    val master = system.actorOf(Props(new Master(noOfWorkers, noOfMessages, noOfElements, listener)), name="master")
    master ! Calculate
  }
}
