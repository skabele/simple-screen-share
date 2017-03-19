package skabele.screenshare.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.libs.json.{JsObject, JsValue}
import WSId._
import WsData._
import WsMessageJson._
import WsActor.SenderEnvelope

object ScreenShareActor {
  def props(name: String, socket: ActorRef) = Props(new ScreenShareActor(name, socket))
}
class ScreenShareActor(override val name: String, override val socket: ActorRef)
  extends Actor with ActorLogging with ChatActor {

  var isReady = false
  var client: Option[ActorRef] = None

  override def handleWsMessage(id: WSId, data: JsObject): Unit = id match {
    case SCREEN_READY =>
      processEmptyData(id, data) { () =>
        isReady = true
        log.info(s"ScreenShareActor $name to BUS: $SCREEN_READY")
        context.system.eventStream.publish(SenderEnvelope(self, WsMessage(SCREEN_READY)))
      }
    case RTC_ICE_CANDIDATE =>
      processData[RTCIceCandidate](id, data) { candidate =>
        client.foreach(_ ! WsMessage(RTC_ICE_CANDIDATE, candidate))
      }
    case RTC_SESSION_DESCRIPTION =>
      processData[RTCSessionDescription](id, data) { description =>
        client.foreach(_ ! WsMessage(RTC_SESSION_DESCRIPTION, description))
      }
    case _ => super.handleWsMessage(id, data)
  }


  def receiveScreenShare: Receive = {
    case SenderEnvelope(clientRef, msg @ WsMessage(CLIENT_READY, _)) =>
      clientRef ! SenderEnvelope(self, WsMessage(SCREEN_READY))
      client = Some(clientRef)
      if (isReady) sendToWS(msg)
    case msg @ WsMessage(RTC_ICE_CANDIDATE, _) =>
      sendToWS(msg)
    case msg @ WsMessage(RTC_SESSION_DESCRIPTION, _) =>
      sendToWS(msg)
  }

  override def receive: Receive = receiveJson orElse receiveChat orElse receiveScreenShare
}