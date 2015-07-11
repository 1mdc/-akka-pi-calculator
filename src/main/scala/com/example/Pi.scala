package com.example

import akka.actor._
import akka.routing.RoundRobinRouter
import scala.concurrent.duration._
import scala.concurrent.duration.Duration

/**
 * Created by hoang on 11/07/15.
 */
object Pi extends App{

  calculate(5, 10000, 10000)

  sealed trait PiMessage
  case object Calculate extends PiMessage
  case class Work(start:Int, noOfElements:Int) extends PiMessage
  case class Result(value:Double) extends PiMessage
  case class PiApproximation(pi:Double, duration:Duration)

  class Worker extends Actor{
    def calculatePiFor(start:Int, noOfElements:Int):Double = (for (i <- start until (start + noOfElements)) yield 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)).sum

    override def receive: Receive = {
      case Work(start, noOfElements) => {
        sender ! Result(calculatePiFor(start, noOfElements))
      }
    }
  }

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

  class Listener extends Actor{
    override def receive: Actor.Receive = {
      case PiApproximation(pi,duration) => {
        println(s"\n\nPi approximation: ${pi} in ${duration} mseconds")
        context.system.shutdown()
      }
    }
  }

  def calculate(noOfWorkers:Int, noOfElements:Int, noOfMessages:Int) {
    val system = ActorSystem("PiSystem")
    val listener = system.actorOf(Props[Listener], name="listener")
    val master = system.actorOf(Props(new Master(noOfWorkers, noOfMessages, noOfElements, listener)), name="master")
    master ! Calculate
  }
}
