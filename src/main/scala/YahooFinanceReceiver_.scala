//package sparkhack
//
//import scala.util.matching.Regex
//import scala.concurrent.duration._
//
//import com.typesafe.config._
//
//import akka.actor.{Actor, ActorRef, Props}
//
//import org.apache.spark.streaming.{Seconds, StreamingContext}
//import org.apache.spark.streaming.StreamingContext._
//import org.apache.spark.streaming.receiver._
//import org.apache.spark.SparkContext._
//
//trait Data {
//  def stock: Stock
//}
//
//case class Stock(id:String) extends AnyVal
//
//object Stocks {
//  private[this] val seq = Seq(
//    Stock("GOOG"),
//    Stock("AAPL"),
//    Stock("ORCL"),
//    Stock("YHOO"),
//    Stock("CSCO"),
//    Stock("INTL"),
//    Stock("AMD"),
//    Stock("IBM"),
//    Stock("MSFT")
//  )
//
//  private[this] val defaults = seq.map(x => (x.id, x)).toMap
//
//  def get(s:String) = defaults.get(s).getOrElse(Stock(s))
//}
//
//case class YahooData(stock:Stock,
//                      trade:Double,
//                      date:String,
//                      time:String,
//                      delta:(Double, Double),
//                      volume:Int)
//object YahooData {
//  def na(s:String, pre:String=>String=identity):Double = if (s == "N/A") Double.MinValue else pre(s).toDouble
//
//  def create(a:Map[String, String]) = {
//    a.get("e1")
//      .flatMap(x => if (x=="N/A") Some(x) else None)
//      .map {_ =>
//      YahooData(
//        stock = Stocks.get(a("s")),
//        trade = na(a("l1")),
//        date = a("d1"),
//        time = a("t1"),
//        delta = (na(a("c6")), na(a("p2"), _.init.mkString)),
//        volume = na(a("v")).toInt
//      )
//    }
//  }
//}
//
//class Yahoo(feeder:String) extends Serializable {
//
//  def apply(stocks:Seq[Stock])(implicit @transient ssc:StreamingContext) = {
//    ssc.actorStream[YahooData](Props(new YahooActorReceiver(feeder, stocks)), "YahooReceiver")
//  }
//}
//
//class YahooActorReceiver(feeder:String, stocks:Seq[Stock]) extends Actor with ActorHelper {
//
//  // cache here the last change for each stock
//  // then not push the block if it didn't changed...
//  var lasts:Map[String, YahooData] = Map.empty
//
//  def receive = {
//    case y:YahooData ⇒ {
//      val push = lasts
//        .get(y.stock.id)
//        .map(_ != y)
//        .getOrElse(true)
//
//      lasts = lasts + (y.stock.id -> y)
//
//      if (push) {
//        pushBlock(y)
//      }
//    }
//  }
//
//  override def postStop() = () //remotePublisher ! UnsubscribeReceiver(context.self)
//}
//
//
//object FeederActor {
//  case class Start(sender: Actor, stocks: Seq[Stock])
//}
//
//class FeederActor extends Actor {
//  import java.net.URL
//
//  // http://cliffngan.net/a/13
//  val yahooResponseFormat = List("e1", "s", "l1", "d1", "t1", "c6", "p2", "v")
//  val yahooService        = "http://finance.yahoo.com/d/quotes.csv?s=%s&f=%s&e=.csv";
//
//  def financeData(stocks:Seq[String]) = String.format(yahooService, stocks.mkString(","), yahooResponseFormat.mkString)
//
//  var url:Option[URL] = None
//
//  var subscriber:Option[ActorRef] = None
//
//  def consume(actor:ActorRef, url:URL):Stream[YahooData] = {
//    import java.io.{BufferedReader, InputStreamReader}
//    val b = new BufferedReader(new InputStreamReader(url.openStream, "utf-8"))
//    Stream
//      .continually(b.readLine)
//      .takeWhile(_ != null)
//      .map { l =>
//      YahooData.create((yahooResponseFormat zip l.replace("\"","").split(",")).toMap)
//    }
//      .collect {
//      case Some(y) => y
//    }
//  }
//
//  def receive = {
//
//    case Start(sparkled, stocks)  =>
//      subscriber = Some(sparkled)
//      url = Some(new URL(financeData(stocks.map(_.id))))
//
//    case Tick           =>
//      for {
//        actor <- subscriber
//        u     <- url
//      } consume(actor, u).foreach(actor ! _)
//  }
//}
//
//object Yahoo {
//  import spark.SparkAkka._
//
//  lazy val feeder = actorSystem.actorOf(Props[FeederActor], "FeederActor")
//
//  def start = {
//    actorSystem.scheduler.schedule(0 milliseconds, 500 milliseconds, feeder, Tick)
//
//    actorSystem.
//
//  }
//}