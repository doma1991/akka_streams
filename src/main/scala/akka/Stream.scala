package akka

import akka.stream.scaladsl._

import scala.concurrent._
import akka.http.scaladsl.model.headers._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success}


case class Result(id: String)

case class StripeResponse(has_more: Boolean, data: List[Result])


object JsonFormats extends DefaultJsonProtocol {
  implicit val resultFormat = jsonFormat1(Result)
  implicit val stripeFormat = jsonFormat2(StripeResponse)
}

import JsonFormats._
import spray.json._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

object Stream extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val url = "https://api.stripe.com/v1/balance_transactions?limit=100"

  def buildRequest(id: String) = {
    val urlWithLastId = s"${url}&starting_after=${id}"

    val accessToken = BasicHttpCredentials("sk_test_4eC39HqLyjWDarjtT1zdp7dc", "")
    HttpRequest(HttpMethods.GET, urlWithLastId)
      .addCredentials(accessToken)
  }

  def performRequest(id: String) = {
    Http().singleRequest(buildRequest(id))
      .flatMap { r =>
        val v = Unmarshal(r.entity).to[StripeResponse].zip(Future{"TODO: real body here!!"})
        v
      }
  }

  def performRequestWithPagingResult(id: String) = {
    performRequest(id)
      .map { result => {
        (result._1.data.last.id, result._1.has_more, result._2)
      } }
  }

  val has_more = true
  val id = "txn_1Fqdyl2eZvKYlo2Cn0OSmwaY"

  Source.unfoldAsync(   (id, has_more )    ) {
    case (_, false) => Future.successful(None)
    case (id, has_more) => performRequestWithPagingResult(id).map { result => {
        val (id, has_more, body) = result
        Option((  (id, has_more) -> body))
      }
    }
  }.runForeach(println)


  //  val ids = List("txn_1Fqdyl2eZvKYlo2Cn0OSmwaY", "txn_1Fqdyi2eZvKYlo2CTca8H4od")

  //val source: Source[Int, NotUsed] = Source(1 to 2)

  //  val source = Source(ids)
  //
  //  source
  //    .runForeach( id => {
  //      val responseFuture: Future[HttpResponse] = Http().singleRequest(buildRequest(id))
  //      responseFuture.flatMap { r =>
  //        val body = Unmarshal(r.entity).to[String]
  //        println(s"Response: ${body}")
  //
  //        Unmarshal(r.entity).to[StripeResponse]
  //      }.foreach { s =>
  //        println(s"Has-more: ${s.has_more} Last-Id: ${s.data.last.id}")
  //      }
  //    }
  //  )

  //  val done: Future[Done] = source.runForeach(i => println(i))
  //  done.onComplete(_ => system.terminate())


  //  val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
  //
  //
  //  responseFuture.flatMap { r =>
  //    val body = Unmarshal(r.entity).to[String]
  //    println(s"Response: ${body}")
  //
  //    Unmarshal(r.entity).to[StripeResponse]
  //  }.foreach { s =>
  //    println(s"Has-more: ${s.has_more} Last-Id: ${s.data.last.id}")
  //  }


  //  responseFuture
  //    .onComplete {
  //      case Success(value) => {
  //        val stripeResult: Future[StripeResponse] = Unmarshal(value.entity).to[StripeResponse]
  //
  //        println("LastId: " )
  //      }
  //      case Failure(_)   => sys.error("something wrong")
  //    }


}
