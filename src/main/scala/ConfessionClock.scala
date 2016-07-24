import java.io.{File, FileInputStream}
import java.util
import java.util.TimerTask

import twitter4j.{Query, Status, TwitterFactory}
import twitter4j.conf.ConfigurationBuilder

import scala.collection.JavaConversions._
import com.github.nscala_time.time.Imports._
import org.yaml.snakeyaml.Yaml

/**
  * Created by frank on 24/07/16.
  */
object ConfessionClock extends App {
  implicit def functionToTimerTask(f: () => Unit): TimerTask = {
    new TimerTask {
      def run() = f()
    }
  }

  implicit def anyToString(a: Any): String = {
    a.toString
  }

  class Timer extends util.Timer {
    def schedule(task: TimerTask, interval: Int): Unit = {
      super.schedule(task, 0L, interval * 1000.toLong)
    }
  }

  object TwitterSearcher {
    val credentials = new Yaml().load(new FileInputStream(new File("credentials.yml"))).asInstanceOf[util.Map[String, Any]]
    val config = new ConfigurationBuilder()
      .setOAuthConsumerKey(credentials.get("consumerKey"))
      .setOAuthConsumerSecret(credentials.get("consumerSecret"))
      .setOAuthAccessToken(credentials.get("oauthKey"))
      .setOAuthAccessTokenSecret(credentials.get("oauthSecret"))
      .build()
    val twitter = new TwitterFactory(config).getInstance

    def search(implicit queryString: String): List[Status] = {
      twitter.search(new Query(queryString)).getTweets.toList
    }
  }

  trait Notifiable {
    def apply(text: String)
  }

  object MacDesktopNotifier extends Notifiable {
    val runtime = Runtime.getRuntime

    def apply(text: String) = {
      val command = s"""display notification "$text" with title "Confession clock""""
      runtime.exec(Array("osascript", "-e", command))
    }
  }

  object PrintlnNotifier extends Notifiable {
    def apply(text: String) = {
      println(text);
    }
  }

  class ConfessionQuery {
    val now = DateTime.now()
    val time = now.toString("h:mm")
    val start = now.minusDays(5).toString("yyyy-MM-dd")
    val end = now.toString("yyyy-MM-dd")

    override def toString(): String = {
      s"""and "it's $time am" since:$start until:$end"""
    }
  }

  override def main(args: Array[String]) = {
    val notifiers = List[Notifiable](MacDesktopNotifier, PrintlnNotifier)
    val task = () => {
      TwitterSearcher.search(new ConfessionQuery).foreach((status: Status) => {
        notifiers.foreach {
          _.apply(status.getText)
        }
      })
    }

    new Timer().schedule(task, 60)
  }
}
