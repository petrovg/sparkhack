package sparkhack

import java.net.URL

import scala.util.matching.Regex
import scala.concurrent.duration._

import com.typesafe.config._

import akka.actor.{Actor, ActorRef, Props, ActorSystem}

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.receiver._
import org.apache.spark.SparkContext._
import scala.concurrent.ExecutionContext.Implicits.global

trait Data {
  def stock: Stock
}

case class Stock(id:String) extends AnyVal

object Stocks {
  val seq = Seq(
    Stock("GOOG"),
    Stock("AAPL"),
    Stock("ORCL"),
    Stock("YHOO"),
    Stock("CSCO"),
    Stock("INTL"),
    Stock("AMD"),
    Stock("IBM"),
    Stock("MSFT")
  )

  private[this] val defaults = seq.map(x => (x.id, x)).toMap

  def get(s:String) = defaults.get(s).getOrElse(Stock(s))
}

case class YahooData(stock:Stock,
                      trade:Double,
                      date:String,
                      time:String,
                      delta:(Double, Double),
                      volume:Int)
object YahooData {
  def na(s:String, pre:String=>String=identity):Double = if (s == "N/A") Double.MinValue else pre(s).toDouble

  def create(a:Map[String, String]) = {
    a.get("e1")
      .flatMap(x => if (x=="N/A") Some(x) else None)
      .map {_ =>
      YahooData(
        stock = Stocks.get(a("s")),
        trade = na(a("l1")),
        date = a("d1"),
        time = a("t1"),
        delta = (na(a("c6")), na(a("p2"), _.init.mkString)),
        volume = na(a("v")).toInt
      )
    }
  }
}


object YahooFinance {

  // http://cliffngan.net/a/13
  val yahooResponseFormat = List("e1", "s", "l1", "d1", "t1", "c6", "p2", "v")
  val yahooService        = "http://finance.yahoo.com/d/quotes.csv?s=%s&f=%s&e=.csv";

  def financeData(stocks:Seq[Stock]): URL = new URL(String.format(yahooService, stocks.map(_.id).mkString(","), yahooResponseFormat.mkString))

  def consume(url:URL):Stream[YahooData] = {
    import java.io.{BufferedReader, InputStreamReader}
    val b = new BufferedReader(new InputStreamReader(url.openStream, "utf-8"))
    Stream
      .continually(b.readLine)
      .takeWhile(_ != null)
      .map { l =>
      YahooData.create((yahooResponseFormat zip l.replace("\"","").split(",")).toMap)
    }.flatten
  }

}


object FeederActor {
  case object Tick
}

class FeederActor(stocks: Seq[Stock]) extends Actor with ActorHelper {
  import java.net.URL

  import FeederActor._

  import YahooFinance._

  override def preStart(): Unit = {
    context.system.scheduler.schedule(1 second, 3 seconds, context.self, FeederActor.Tick)
  }


  def receive = {
    case Tick =>
      val data: Stream[YahooData] = consume(financeData(stocks))
      println("Hi Deenar!!! >>>> " + data)
      store(data)
  }
}

object YahooReceiver {

  val system = ActorSystem("Stocks")

  lazy val feeder = system.actorOf(Props(new FeederActor(Stocks.seq)), "FeederActor")

  def start = {
    system.scheduler.schedule(0 milliseconds, 3 seconds, feeder, FeederActor.Tick)
  }

}