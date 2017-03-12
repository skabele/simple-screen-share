package skabele.screenshare.actors

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import play.api.libs.json._

import scala.concurrent.duration._

class WsActorSpec extends TestKit(ActorSystem("WsActorSpec", ConfigFactory.parseString("""
    akka.loggers = ["akka.testkit.TestEventListener"]
  """))) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class Helper {
    val duration = 300.millis
    val socket = TestProbe()

    class BareWsActor(override val socket: ActorRef) extends WsActor {
      override def receive = receiveJson
    }
    val wsActor = system.actorOf(Props(new BareWsActor(socket.ref)))

    def expectErrorMessage(probe: TestProbe): Unit = probe.expectMsgPF(duration) {
      case json: JsObject if json.value.get("msg").contains(JsString("ERROR")) =>
    }
  }

  "WsActor" should {

    "report error on external message without msg property" in new Helper {
      wsActor ! Json.parse(""" {"foo": "bar"} """)
      expectErrorMessage(socket)
    }

    "report error on external message of unknown type" in new Helper {
      wsActor ! Json.parse(""" {"msg": "NONEXISTENT-MESSAGE-TYPE"} """)
      expectErrorMessage(socket)
    }

    "report error on external message of known type but with wrong JSON data" in new Helper {
      wsActor ! Json.parse(""" {"msg": "ERROR", "foo": "bar"} """)
      expectErrorMessage(socket)
    }

    "log warning on ERROR ws message" in new Helper {
      val errMsg = "Some error message"
      EventFilter.warning(pattern = errMsg, occurrences = 1) intercept {
        wsActor ! Json.parse(s""" {"msg": "ERROR", "text": "$errMsg"} """)
      }
    }

  }

}
