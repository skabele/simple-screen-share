package skabele.screenshare.actors

import akka.actor.{ActorRef, ActorSystem, Props}
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
      system.eventStream.subscribe(eventStreamProbe.ref, classOf[ChatEnvelope])
      chatActor ! Json.parse(""" {"msg": "CHAT", "text": "foo bar baz"} """)
      eventStreamProbe.expectMsg(duration, ChatEnvelope(chatActor, ChatMessage(ChatMessage.msg, "foo bar baz")))
      socket.expectNoMsg()
    }

    "on ChatEnvelope send CHAT message to socket" in new Helper {
      val chatMessage = ChatMessage(ChatMessage.msg, "Ignore me!")
      val sender = TestProbe()
      chatActor ! ChatEnvelope(sender.ref, chatMessage)
      socket.expectMsg(duration, Json.toJson(chatMessage))
      sender.expectNoMsg()
    }

    "ignore ChatEnvelope originated from this actor" in new Helper {
      val chatMessage = ChatMessage(ChatMessage.msg, "Ignore me!")
      chatActor ! ChatEnvelope(chatActor, chatMessage)
      socket.expectNoMsg(duration)
    }

  }

}
