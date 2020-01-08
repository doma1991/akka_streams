package akka

import java.text.SimpleDateFormat
import akka.actor.ActorSystem
import com.jayway.jsonpath.JsonPath
import spray.json.DefaultJsonProtocol

case class Result(id: String)

case class StripeResponse(has_more: Boolean, data: List[Result])


object JsonFormats extends DefaultJsonProtocol {
  implicit val resultFormat = jsonFormat1(Result)
  implicit val stripeFormat = jsonFormat2(StripeResponse)
}

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}



object App extends App {

  val idFromJson = (s: String) => JsonPath.read[String](s, "$.data[-1].id")
  val hasMoreFromJson = (s: String) => JsonPath.read[Boolean](s, "$.has_more")

  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher
  implicit val formats = DefaultFormats

  def epochToDate(epochMillis: BigInt): String = {
    val convertedToLong = epochMillis * 1000L
    val df:SimpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    df.format(convertedToLong)
  }

  val stripe = new Stream("txn_1Fqdyl2eZvKYlo2Cn0OSmwaY", "starting_after", hasMoreFromJson, 100, idFromJson)

  stripe.asSource().runForeach(id => {

    val rawValue = (parse(id) \ "data" \\ "created").toOption
    rawValue match {

      case l: Option[JObject] => l match { case Some(j) => j.obj.foreach(i => i._2 match { case JInt(int) => println("Date: " + epochToDate(int))})}
      case None => println("Value not present in JSON")
    }

//    println(rawValue)
  })
}
