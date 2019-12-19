package akka

import akka.JsonFormats.{jsonFormat1, jsonFormat2}
import akka.actor.ActorSystem
import com.jayway.jsonpath.JsonPath
import spray.json.DefaultJsonProtocol



case class Result(id: String)

case class StripeResponse(has_more: Boolean, data: List[Result])


object JsonFormats extends DefaultJsonProtocol {
  implicit val resultFormat = jsonFormat1(Result)
  implicit val stripeFormat = jsonFormat2(StripeResponse)
}

import JsonFormats._
import spray.json._


object App extends App {

  val idFromJson = (s: String) => JsonPath.read[String](s, "$.data[-1].id")
  val hasMoreFromJson = (s: String) => JsonPath.read[Boolean](s, "$.has_more")

  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher
  val stripe = new Stream("txn_1Fqdyl2eZvKYlo2Cn0OSmwaY", "starting_after", hasMoreFromJson, 100, idFromJson)

  stripe.asSource().runForeach(println)
}
