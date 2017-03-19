package skabele.screenshare.actors

import akka.actor._
import akka.testkit._
import org.scalatest._
import play.api.libs.json._
import WSId._
import WsData._
import WsMessageJson._
import WsActor.SenderEnvelope

import scala.concurrent.duration._

class ChatActorSpec extends TestKit(ActorSystem("ChatActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class Helper {
    val duration = 300.millis
    val socket = TestProbe()

    class BareChatActor(override val socket: ActorRef) extends ChatActor {
      def name = "Test"
      override def receive = receiveJson orElse receiveChat
    }
    val chatActor = system.actorOf(Props(new BareChatActor(socket.ref)))
  }

  "ChatActor" should {

    "forward external CHAT message to eventStream" in new Helper {
      val eventStreamProbe = TestProbe()
      system.eventStream.subscribe(eventStreamProbe.ref, classOf[SenderEnvelope])
      chatActor ! Json.parse(""" {"id": "CHAT", "data": {"text": "foo bar baz"} } """)
      eventStreamProbe.expectMsg(duration, SenderEnvelope(chatActor, WsMessage(CHAT,Chat("foo bar baz"))))
      socket.expectNoMsg()
    }

    "on ChatEnvelope send CHAT message to socket" in new Helper {
      val chatMessage = WsMessage(CHAT,Chat("Read me"))
      val sender = TestProbe()
      chatActor ! SenderEnvelope(sender.ref, chatMessage)
      socket.expectMsg(duration, Json.toJson(chatMessage))
      sender.expectNoMsg()
    }

    "ignore ChatEnvelope originated from this actor" in new Helper {
      val chatMessage = WsMessage(CHAT,Chat("Ignore me"))
      chatActor ! SenderEnvelope(chatActor, chatMessage)
      socket.expectNoMsg(duration)
    }

  }

}
