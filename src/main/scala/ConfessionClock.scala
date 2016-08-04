import java.io.{File, FileInputStream}
import java.util
import java.util.TimerTask

import twitter4j.{Query, Status, TwitterFactory}
import twitter4j.conf.ConfigurationBuilder

import scala.collection.JavaConversions._
import com.github.nscala_time.time.Imports._
import org.yaml.snakeyaml.Yaml

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.swing._

/**
  * Created by frank on 24/07/16.
  */
object ConfessionClock extends App {
  val task = new TimerTask {
    override def run(): Unit = {
      val search: Future[List[Status]] = Future {
        TwitterSearcher.search(new ConfessionQuery)
      }
      search onComplete {
        case Success(stati) => NotifyAll(stati)
        case _ =>
      }
    }
  }
  new Timer().schedule(task, 10)


  object UI extends MainFrame {
    val panel = new BoxPanel(Orientation.Vertical) {
      border = Swing.EmptyBorder(10, 10, 10, 10)
    }
    def addTweet(text: String) = {
      val label = new Label(text)
      println(label)

      panel.contents += label
      panel.repaint
    }

    title = "Confession Clock"
    visible = true
    preferredSize = new Dimension(500, 500)
    contents = panel

  }


  implicit def anyToString(a: Any): String = {
    a.toString
  }

  class Timer extends util.Timer {
    def schedule(task: TimerTask, interval: Int) = {
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

    def search(queryString: String): List[Status] = {
      twitter.search(new Query(queryString)).getTweets.toList
    }
  }

  class ConfessionQuery {
    val now = DateTime.now()
    val time = now.toString("h:mm")
    val start = now.minusDays(7).toString("yyyy-MM-dd")
    val end = now.toString("yyyy-MM-dd")

    override def toString(): String = {
      s"""and "it's $time am" since:$start until:$end"""
    }
  }

  object NotifyAll {
    val notifiers = List[Notifiable](GuiNotifier)

    def apply(stati: List[Status]) {
      stati.foreach((status: Status) => {
        notifiers.foreach((notifier: Notifiable) => {
          Future {
            notifier(status.getText)
          }
        })
      })

    }
  }

  trait Notifiable {
    def apply(text: String)
  }

  object GuiNotifier extends Notifiable {
    def apply(text: String) = {
      UI.addTweet(text)
    }
  }

  object PrintlnNotifier extends Notifiable {
    def apply(text: String) = {
      println(text)
    }
  }
}
