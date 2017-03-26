package skabele.screenshare.actors

import akka.actor.{ActorRef, Props, Terminated}
import WsId._
import WsData._
import InternalMessage._
import akka.event.LoggingReceive

object ClientActor {
  def props(socket: ActorRef) = Props(new ClientActor(socket))

}

class ClientActor(override val socket: ActorRef) extends ChatActor {
  var name = "Anonymous Client"
  var screen: Option[ActorRef] = None
  var isReady = false

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[ScreenReadyToShare])
    super.preStart()
  }

  def receiveClient: Receive = {
    case WsMessage(CLIENT_READY, ClientReady(_name)) =>
      if (isReady) {
        logWarnAndSendErrorToWS(s"Client is already ready with name='$name'")
      } else {
        isReady = true
        name = _name
        context.system.eventStream.publish(ClientReadyToListen(self, name, isResponse = false))
      }

    case WsMessage(RTC_ICE_CANDIDATE, candidate: RTCIceCandidate) =>
      screen.foreach(_ ! IceCandidate(candidate))

    case WsMessage(RTC_SESSION_DESCRIPTION, session: RTCSessionDescription) =>
      screen.foreach(_ ! Session(session))

    case ScreenReadyToShare(ref, screenName, isResponse) =>
      screen = Some(ref)
      context.watch(ref)
      if (!isResponse) {
        ref ! ClientReadyToListen(self, name, isResponse = true)
      }
      if (isReady) {
        socket ! WsMessage(SCREEN_READY, ScreenReady(screenName))
      }

    case NameAlreadyTaken() =>
      socket ! WsMessage(NAME_ALREADY_TAKEN, NoData())
      context.stop(self)

    case Terminated(ref) if screen.contains(ref) =>
      socket ! WsMessage(SCREEN_LEFT, NoData())
      screen = None

    case IceCandidate(candidate) =>
      socket ! WsMessage(RTC_ICE_CANDIDATE, candidate)

    case Session(session) =>
      socket ! WsMessage(RTC_SESSION_DESCRIPTION, session)
  }

  override def receive: Receive = LoggingReceive {
    receiveClient orElse receiveChat orElse receiveError
  }
}
