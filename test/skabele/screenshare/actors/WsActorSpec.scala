package skabele.screenshare.actors

import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import play.api.libs.json._
import WsData._
import WsId._

import scala.concurrent.duration._

class WsActorSpec extends TestKit(ActorSystem("WsActorSpec", ConfigFactory.parseString("""
    akka.loggers = ["akka.testkit.TestEventListener"]
  """))) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class Helper {
    val duration = 100.millis
    val socket = TestProbe()

    class BareWsActor(override val socket: ActorRef) extends WsActor {
      override def receive = receiveError
      var name = "Test"
    }
    val wsActor = system.actorOf(Props(new BareWsActor(socket.ref)))

    def expectServerError(probe: TestProbe): Unit = probe.expectMsgPF(duration) {
      case WsMessage(SERVER_ERROR, _) =>
    }
  }

  "WsActor" should {

    "log warning on FRONTEND_ERROR" in new Helper {
      val errMsg = "Some error message"
      EventFilter.warning(pattern = errMsg, occurrences = 1) intercept {
        wsActor ! WsMessage(FRONTEND_ERROR, Error(errMsg))
      }
    }

    "log warning and publish SERVER_ERROR on PARSE_ERROR" in new Helper {
      val jsonAsStr = """{"foo": "bar"}"""
      EventFilter.warning(pattern = "none of recognised messages", occurrences = 1) intercept {
        wsActor ! WsMessage(PARSE_ERROR, Error(jsonAsStr))
      }
      expectServerError(socket)
    }

    "log error on unhandled message" in new Helper {
      case class UnhandledMessage()
      EventFilter.error(pattern = "unhandled message", occurrences = 1) intercept {
        wsActor ! UnhandledMessage
      }
    }

  }

}
