package skabele.screenshare.actors

import akka.actor.ActorSystem
import akka.testkit._
import org.scalatest._
import play.api.libs.json._
import skabele.screenshare.actors.ChatActor.{ChatEnvelope, ChatMessage}

import scala.concurrent.duration._

class ChatActorSpec extends TestKit(ActorSystem("ChatActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val duration = 300.millis

  "ChatActor" should {

    def expectErrorMessage(probe: TestProbe): Unit = probe.expectMsgPF(duration) {
      case json: JsObject if json.value.get("msg").contains(JsString("ERROR")) =>
    }

    "report error on external message without msg property" in {
      val socket = TestProbe()
      val chatActor = system.actorOf(ChatActor.props("Test", socket.ref))
      chatActor ! Json.parse(""" {"foo": "bar"} """)
      expectErrorMessage(socket)
    }

    "report error on external message of unknown type" in {
      val socket = TestProbe()
      val chatActor = system.actorOf(ChatActor.props("Test", socket.ref))
      chatActor ! Json.parse(""" {"msg": "NONEXISTENT-MESSAGE-TYPE"} """)
      expectErrorMessage(socket)
    }

    "report error on external message of known type but with wrong JSON data" in {
      val socket = TestProbe()
      val chatActor = system.actorOf(ChatActor.props("Test", socket.ref))
      chatActor ! Json.parse(""" {"msg": "CHAT", "foo": "bar"} """)
      expectErrorMessage(socket)
    }

    "forward external CHAT message to eventStream" in {
      val eventStreamProbe = TestProbe()
      system.eventStream.subscribe(eventStreamProbe.ref, classOf[ChatEnvelope])
      val socket = TestProbe()
      val chatActor = system.actorOf(ChatActor.props("Test", socket.ref))
      chatActor ! Json.parse(""" {"msg": "CHAT", "text": "foo bar baz"} """)
      eventStreamProbe.expectMsg(duration, ChatEnvelope(chatActor, ChatMessage(ChatMessage.msg, "foo bar baz")))
      socket.expectNoMsg()
    }

    "on ChatEnvelope send CHAT message to socket" in {
      val socket = TestProbe()
      val chatActor = system.actorOf(ChatActor.props("Test", socket.ref))
      val chatMessage = ChatMessage(ChatMessage.msg, "Ignore me!")
      val sender = TestProbe()
      chatActor ! ChatEnvelope(sender.ref, chatMessage)
      socket.expectMsg(duration, Json.toJson(chatMessage))
      sender.expectNoMsg()
    }

    "ignore ChatEnvelope originated from this actor" in {
      val socket = TestProbe()
      val chatActor = system.actorOf(ChatActor.props("Test", socket.ref))
      val chatMessage = ChatMessage(ChatMessage.msg, "Ignore me!")
      chatActor ! ChatEnvelope(chatActor, chatMessage)
      socket.expectNoMsg(duration)
    }

  }

}
