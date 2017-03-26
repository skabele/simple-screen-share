package skabele.screenshare.actors

import akka.actor._
import akka.testkit._
import org.scalatest._
import WsData._
import WsId._
import InternalMessage.{ScreenReadyToShare, _}

import scala.concurrent.duration._

class ClientActorSpec extends TestKit(ActorSystem("ClientActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class Helper {
    val duration = 100.millis
    val socket = TestProbe()
    val clientActor = system.actorOf(Props(new ClientActor(socket.ref)))

    val screen = TestProbe()

    val clientActorWithScreen = system.actorOf(Props(new ClientActor(socket.ref)))
    clientActorWithScreen ! ScreenReadyToShare(screen.ref, "Screen", isResponse = true)

    val candidate = RTCIceCandidate("foo", 1, "bar")
    val session = RTCSessionDescription("foo", "bar")

  }

  "ClientActor" should {

    "on CLIENT_READY publish ClientReadyToListen to eventStream" in new Helper {
      val eventStreamProbe = TestProbe()
      system.eventStream.subscribe(eventStreamProbe.ref, classOf[ClientReadyToListen])
      clientActor ! WsMessage(CLIENT_READY, ClientReady("John Doe"))
      eventStreamProbe.expectMsg(duration, ClientReadyToListen(clientActor, "John Doe", isResponse = false))
    }

    "on RTC_ICE_CANDIDATE send IceCandidate to screen actor" in new Helper {
      clientActorWithScreen ! WsMessage(RTC_ICE_CANDIDATE, candidate)
      screen.expectMsg(duration, IceCandidate(candidate))
    }

    "on RTC_SESSION_DESCRIPTION send Session to screen actor" in new Helper {
      clientActorWithScreen ! WsMessage(RTC_SESSION_DESCRIPTION, session)
      screen.expectMsg(duration, Session(session))
    }

    "(when ready) on ScreenReadyToShare(isResponse = false) send back ClientReadyToListen and SCREEN_READY to socket" in new Helper {
      clientActor ! WsMessage(CLIENT_READY, ClientReady("John Doe"))
      clientActor ! ScreenReadyToShare(screen.ref, "Screen", isResponse = false)
      screen.expectMsg(duration, ClientReadyToListen(clientActor, "John Doe", isResponse = true))
      socket.expectMsg(duration, WsMessage(SCREEN_READY, ScreenReady("Screen")))
    }

    "(when not ready) on ScreenReadyToShare(isResponse = true) send message neither to screen nor to socket" in new Helper {
      clientActor ! ScreenReadyToShare(screen.ref, "Screen", isResponse = true)
      screen.expectNoMsg(duration)
      socket.expectNoMsg(duration)
    }

    "on NameAlreadyTaken send NAME_ALREADY_TAKEN to socket and terminate self" in new Helper {
      val clientWatcher = TestProbe()
      clientWatcher.watch(clientActor)
      clientActor ! NameAlreadyTaken()
      socket.expectMsg(duration, WsMessage(NAME_ALREADY_TAKEN, NoData()))
      clientWatcher.expectTerminated(clientActor, duration)
    }

    "on ScreenReadyToShare start watching screen and on its termination send SCREEN_LEFT to socket" in new Helper {
      val fakeScreen = system.actorOf(Props(new Actor {
        def receive = { case "foo" => }
      }))
      clientActor ! ScreenReadyToShare(fakeScreen, "Screen", isResponse = true)
      fakeScreen ! PoisonPill
      socket.expectMsg(duration, WsMessage(SCREEN_LEFT, NoData()))
   }

    "on IceCandidate send RTC_ICE_CANDIDATE to socket" in new Helper {
      clientActor ! IceCandidate(candidate)
      socket.expectMsg(duration, WsMessage(RTC_ICE_CANDIDATE, candidate))
    }

    "on Session send RTC_SESSION_DESCRIPTION to socket" in new Helper {
      clientActor ! Session(session)
      socket.expectMsg(duration, WsMessage(RTC_SESSION_DESCRIPTION, session))
    }

  }
}
