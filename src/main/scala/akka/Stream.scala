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

import JsonFormats._
import spray.json._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

class Stream(nextId: String, queryParam: String, hasMore: String => Boolean, limit: Int, nextIdFromJson: String => String) {

  // nextId: fromJson("data[last].id")
  // queryParam: starting_after
  // hasMore: has_more
  // limit: 100

  val jsonPathExpresssion = "data[data,length-1].id" // TODO: right jsonpath syntax

  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val url = s"https://api.stripe.com/v1/balance_transactions?"
  val numberOfResults = s"limit=${limit}"

  def buildRequest(id: String) = {
    val urlWithLastId = s"${url}${numberOfResults}&${queryParam}=${id}"

    val accessToken = BasicHttpCredentials("sk_test_4eC39HqLyjWDarjtT1zdp7dc", "")
    HttpRequest(HttpMethods.GET, urlWithLastId)
      .addCredentials(accessToken)
  }

  def performRequest(id: String) = {
    Http().singleRequest(buildRequest(id))
      .flatMap { r => Unmarshal(r.entity).to[String]
      }.map { body =>
      (nextIdFromJson(body), hasMore(body), body)
    }
  }

  def performRequestWithPagingResult(id: String) = {
    performRequest(id)
      .map { result => {
        (result._1, result._2, result._3)
      }
      }
  }


  // Retrieve starting point every tick (and store)

  val has_more = true
  val id = "txn_1Fqdyl2eZvKYlo2Cn0OSmwaY"


  def asSource() = {

    Source.unfoldAsync((id, has_more)) {
      case (_, false) => Future.successful(None)
      case (id, true) => performRequestWithPagingResult(id).map { result => {
        val (id, has_more, body) = result
        Option(((id, has_more) -> body))
      }
      }
    }
}
  //.runForeach(println)


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
