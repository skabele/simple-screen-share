package skabele.screenshare.actors

import akka.actor.{ActorRef, Props}
import play.api.libs.json.{JsObject, JsValue}
import WSId._
import WsData._
import WsMessageJson._
import WsActor.SenderEnvelope

object ClientActor {
  def props(name: String, socket: ActorRef) = Props(new ClientActor(name, socket))
}

class ClientActor(override val name: String, override val socket: ActorRef) extends ChatActor {

  var isReady = false
  var screen: Option[ActorRef] = None

  override def handleWsMessage(id: WSId, data: JsObject): Unit = id match {
    case CLIENT_READY =>
      processEmptyData(id, data) { () =>
        isReady = true
        log.info(s"Client $name to BUS: $CLIENT_READY")
        context.system.eventStream.publish(SenderEnvelope(self, WsMessage(CLIENT_READY)))
      }
    case RTC_ICE_CANDIDATE  =>
      processData[RTCIceCandidate](id, data) { candidate =>
        screen.foreach(_ ! WsMessage(RTC_ICE_CANDIDATE, candidate))
      }
    case RTC_SESSION_DESCRIPTION =>
      processData[RTCSessionDescription](id, data) { description =>
        screen.foreach(_ ! WsMessage(RTC_SESSION_DESCRIPTION, description))
      }
    case _ => super.handleWsMessage(id, data)
  }

  def receiveClient: Receive = {
    case SenderEnvelope(screenRef, msg @ WsMessage(SCREEN_READY, _)) =>
      screen = Some(screenRef)
      if (isReady) {
        log.info(s"ClientActor $name to screen: ClientReady")
        screenRef ! SenderEnvelope(self, msg)
      }
    case msg @ WsMessage(SCREEN_READY, _) =>
      sendToWS(msg)
    case msg @ WsMessage(RTC_ICE_CANDIDATE, _) =>
      sendToWS(msg)
    case msg @ WsMessage(RTC_SESSION_DESCRIPTION, _) =>
      sendToWS(msg)
  }

  override def receive: Receive = receiveJson orElse receiveChat orElse receiveClient
}
