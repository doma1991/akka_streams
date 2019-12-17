package akka

import akka.stream.scaladsl._

import scala.concurrent._
import akka.http.scaladsl.model.headers._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.util.{Failure, Success}


object Stream extends App {

  val url = "https://api.stripe.com/v1/balance_transactions?limit=3"
  val accessToken = BasicHttpCredentials("sk_test_4eC39HqLyjWDarjtT1zdp7dc", "")
  val request = HttpRequest(HttpMethods.GET, url)
    .addCredentials(accessToken)

  implicit val system = ActorSystem("QuickStart")
//  val source: Source[Int, NotUsed] = Source(1 to 2)
//  val done: Future[Done] = source.runForeach(i => println(i))
//  done.onComplete(_ => system.terminate())

  implicit val ec = system.dispatcher

  val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

  responseFuture
    .onComplete {
      case Success(value) => println("OK --> " + Unmarshal(value.entity).to[String])
      case Failure(_)   => sys.error("something wrong")
    }


}
