package skabele.screenshare.actors

import akka.actor.{ActorRef, _}
import WsId._
import WsData._
import InternalMessage._
import akka.event.LoggingReceive

import scala.collection.mutable

object ScreenShareActor {
  def props(socket: ActorRef) = Props(new ScreenShareActor(socket))
}

class ScreenShareActor(override val socket: ActorRef) extends ChatActor {
  var name = "Anonymous Screen"
  var isReady = false

  val clients = mutable.Map[String, ActorRef]()
  def findClientName(ref: ActorRef): Option[String] = clients.find(_._2 == ref).map(_._1)

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[ClientReadyToListen])
    context.system.eventStream.subscribe(self, classOf[ScreenReadyToShare])
    super.preStart()
  }

  def receiveScreenShare: Receive = {
    case WsMessage(SCREEN_READY, ScreenReady(_name)) =>
      if (isReady) {
        logWarnAndSendErrorToWS(s"Screen is already ready with name='$name'")
      } else {
        isReady = true
        name = _name
        clients.get(_name).foreach(_ ! NameAlreadyTaken())
        context.system.eventStream.publish(ScreenReadyToShare(self, name, isResponse = false))
      }

    case WsMessage(RTC_ICE_CANDIDATE_WITH_NAME, RTCIceCandidateWithName(clientName, candidate)) =>
      clients.get(clientName).foreach(_ ! IceCandidate(candidate))

    case WsMessage(RTC_SESSION_DESCRIPTION_WITH_NAME, RTCSessionDescriptionWithName(clientName, session)) =>
      clients.get(clientName).foreach(_ ! Session(session))

    case ClientReadyToListen(ref, clientName, isResponse) =>
      if (clients.isDefinedAt(clientName) || clientName == name) {
        ref ! NameAlreadyTaken()
      } else {
        clients(clientName) = ref
        context.watch(ref)
        if (!isResponse) {
          ref ! ScreenReadyToShare(self, name, isResponse = true)
        }
        if (isReady) {
          socket ! WsMessage(CLIENT_READY, ClientReady(clientName))
        }
      }

    case ScreenReadyToShare(ref, _, _) =>
      if (ref != self) {
        ref ! ScreenAlreadyConnected()
      }

    case ScreenAlreadyConnected() =>
      socket ! WsMessage(SCREEN_ALREADY_CONNECTED, NoData())
      context.stop(self)

    case Terminated(ref) =>
      findClientName(ref).foreach { clientName =>
        socket ! WsMessage(CLIENT_LEFT, ClientLeft(clientName))
        clients -= clientName
      }

    case IceCandidate(candidate) =>
      findClientName(sender).foreach { clientName =>
        socket ! WsMessage(RTC_ICE_CANDIDATE_WITH_NAME, RTCIceCandidateWithName(clientName, candidate))
      }

    case Session(session) =>
      findClientName(sender).foreach { clientName =>
        socket ! WsMessage(RTC_SESSION_DESCRIPTION_WITH_NAME, RTCSessionDescriptionWithName(clientName, session))
      }

  }

  override def receive: Receive = LoggingReceive {
    receiveScreenShare orElse receiveChat orElse receiveError
  }
}