package skabele.screenshare.actors

import akka.actor._
import akka.testkit._
import org.scalatest._
import WsData._
import WsId._
import InternalMessage._

import scala.concurrent.duration._

class ChatActorSpec extends TestKit(ActorSystem("ChatActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class Helper {
    val duration = 100.millis
    val socket = TestProbe()

    class BareChatActor(override val socket: ActorRef) extends ChatActor {
      var name = "Test"
      override def receive = receiveChat orElse receiveError
    }
    val chatActor = system.actorOf(Props(new BareChatActor(socket.ref)))
  }

  "ChatActor" should {

    "on SEND_CHAT publish ChatPublished to eventStream" in new Helper {
      val eventStreamProbe = TestProbe()
      system.eventStream.subscribe(eventStreamProbe.ref, classOf[ChatPublished])
      val msg = "foo bar baz"
      chatActor ! WsMessage(SEND_CHAT, SendChat(msg))
      eventStreamProbe.expectMsg(duration, ChatPublished(chatActor, "Test", msg))
      socket.expectNoMsg(duration)
    }

    "on ChatPublished send CHAT_MSG to socket" in new Helper {
      val otherActor = TestProbe()
      val msg = "foo bar baz"
      chatActor ! ChatPublished(otherActor.ref, "Test", msg)
      socket.expectMsg(duration, WsMessage(CHAT_MSG, ChatMsg("Test", msg)))
      socket.expectNoMsg(duration)
    }

    "if ChatPublished published by itself" in new Helper {
      val msg = "foo bar baz"
      chatActor ! ChatPublished(chatActor, "Test", msg)
      socket.expectNoMsg(duration)
    }

  }

}
