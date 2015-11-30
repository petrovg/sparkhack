import org.apache.spark._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import sparkhack._
import akka.actor._
import scala.concurrent.duration._

val sparkConf = new SparkConf().setAppName("Stocks").setMaster("local[4]")
val ssc = new StreamingContext(sparkConf, Seconds(5))

val lines = ssc.actorStream[YahooData](Props(new FeederActor(Stocks.seq)), "StocksReceive")

lines.foreach( rdd => println("BOO! === >>>> " + rdd.collect) )

StreamingContext.getActive.foreach{ _.stop(stopSparkContext = false) }


//sys.scheduler.schedule(0.milliseconds, 3.seconds, sys.actorSelection("FeederActor"), FeederActor.Tick)