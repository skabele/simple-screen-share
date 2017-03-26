package skabele.screenshare.actors

import akka.actor._
import akka.testkit._
import org.scalatest._
import WsData._
import WsId._
import InternalMessage.{ScreenReadyToShare, _}

import scala.concurrent.duration._

class ScreenShareActorSpec extends TestKit(ActorSystem("ClientActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class Helper {
    val duration = 100.millis
    val socket = TestProbe()

    val candidate = RTCIceCandidate("foo", 1, "bar")
    val session = RTCSessionDescription("foo", "bar")

    def withScreenActor(test: ActorRef => Unit): Unit = {
      val screenActor = system.actorOf(Props(new ScreenShareActor(socket.ref)))
      test(screenActor)
      system.stop(screenActor)
    }

    val client1 = TestProbe()
    val client2 = TestProbe()
    val client1Name = "Client 1"
    val client2Name = "Client 2"

    def withScreenActorWithClients(test: ActorRef => Unit): Unit = {
      val screenActor = system.actorOf(Props(new ScreenShareActor(socket.ref)))
      screenActor ! ClientReadyToListen(client1.ref, client1Name, isResponse = true)
      screenActor ! ClientReadyToListen(client2.ref, client2Name, isResponse = true)
      test(screenActor)
      system.stop(screenActor)
    }

  }

  "ClientActor" should {

    "on SCREEN_READY publish ScreenReadyToShare to eventStream" in new Helper {
      val eventStreamProbe = TestProbe()
      system.eventStream.subscribe(eventStreamProbe.ref, classOf[ScreenReadyToShare])
      withScreenActor { screenActor =>
        screenActor ! WsMessage(SCREEN_READY, ScreenReady("John Doe"))
        eventStreamProbe.expectMsg(duration, ScreenReadyToShare(screenActor, "John Doe", isResponse = false))
      }
    }

    "on RTC_ICE_CANDIDATE_WITH_NAME send IceCandidate to named client actor(s)" in new Helper {
      withScreenActorWithClients { screenActor =>
        screenActor ! WsMessage(RTC_ICE_CANDIDATE_WITH_NAME, RTCIceCandidateWithName(client1Name, candidate))
        client1.expectMsg(duration, IceCandidate(candidate))
        client2.expectNoMsg(duration)
      }
    }

    "on RTC_SESSION_DESCRIPTION_WITH_NAME send Session to named client actor" in new Helper {
      withScreenActorWithClients { screenActor =>
        screenActor ! WsMessage(RTC_SESSION_DESCRIPTION_WITH_NAME, RTCSessionDescriptionWithName(client1Name, session))
        client1.expectMsg(duration, Session(session))
        client2.expectNoMsg(duration)
      }
    }

    "(when ready) on ClientReadyToListen(isResponse = false) send back ScreenReadyToShare and CLIENT_READY to socket" in new Helper {
      withScreenActor { screenActor =>
        screenActor ! WsMessage(SCREEN_READY, ScreenReady("Screen"))
        screenActor ! ClientReadyToListen(client1.ref, client1Name, isResponse = false)
        client1.expectMsg(duration, ScreenReadyToShare(screenActor, "Screen", isResponse = true))
        client2.expectNoMsg(duration)
        socket.expectMsg(duration, WsMessage(CLIENT_READY, ClientReady(client1Name)))
      }
    }

    "on ClientReadyToListen with already registered name, send back NameAlreadyTaken" in new Helper {
      withScreenActorWithClients { screenActor =>
        val otherClient = TestProbe()
        screenActor ! ClientReadyToListen(otherClient.ref, client1Name, isResponse = false)
        otherClient.expectMsg(duration, NameAlreadyTaken())
      }
    }

    "on ScreenReadyToShare from another screen, send back ScreenAlreadyConnected" in new Helper {
      withScreenActorWithClients { screenActor =>
        val otherScreen = TestProbe()
        screenActor ! ScreenReadyToShare(otherScreen.ref, client1Name, isResponse = false)
        otherScreen.expectMsg(duration, ScreenAlreadyConnected())
      }
    }

    "(when not ready) on ClientReadyToListen(isResponse = true) send message neither to screen nor to socket" in new Helper {
      withScreenActor { screenActor =>
        screenActor ! ClientReadyToListen(client1.ref, client1Name, isResponse = true)
        client1.expectNoMsg(duration)
        client2.expectNoMsg(duration)
        socket.expectNoMsg(duration)
      }
    }

    "on ScreenAlreadyConnected send SCREEN_ALREADY_CONNECTED to socket and terminate self" in new Helper {
      withScreenActor { screenActor =>
        val screenWatcher = TestProbe()
        screenWatcher.watch(screenActor)
        screenActor ! ScreenAlreadyConnected()
        socket.expectMsg(duration, WsMessage(SCREEN_ALREADY_CONNECTED, NoData()))
        screenWatcher.expectTerminated(screenActor, duration)
      }
    }

    "on ClientReadyToListen start watching client and on its termination send CLIENT_LEFT to socket" in new Helper {
      withScreenActor { screenActor =>
        val fakeClient = system.actorOf(Props(new Actor {
          def receive = { case "foo" => }
        }))
        screenActor ! ClientReadyToListen(fakeClient, "Jane Doe", isResponse = true)
        fakeClient ! PoisonPill
        socket.expectMsg(duration, WsMessage(CLIENT_LEFT, ClientLeft("Jane Doe")))
      }
    }

    "on IceCandidate send RTC_ICE_CANDIDATE_WITH_NAME to socket" in new Helper {
      withScreenActorWithClients { screenActor =>
        screenActor.tell(IceCandidate(candidate), client2.ref)
        socket.expectMsg(duration, WsMessage(RTC_ICE_CANDIDATE_WITH_NAME, RTCIceCandidateWithName(client2Name, candidate)))
      }
    }

    "on Session send RTC_SESSION_DESCRIPTION_WITH_NAME to socket" in new Helper {
      withScreenActorWithClients { screenActor =>
        screenActor.tell(Session(session), client2.ref)
        socket.expectMsg(duration, WsMessage(RTC_SESSION_DESCRIPTION_WITH_NAME, RTCSessionDescriptionWithName(client2Name, session)))
      }
    }

  }
}
