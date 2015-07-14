# Calculating Pi by using Akka

This example is to demonstrate the powerful of akka in doing tasks in parallel. The implementation calculates Pi number by following algorithm: pi/4 = 1/1 - 1/3 + 1/5 - 1/7 + .... So by using Akka, we can devide this formula into chunks and process them in parallel in worker actors then send back to master actor for accumulating the final result.