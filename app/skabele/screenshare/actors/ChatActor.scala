package skabele.screenshare.actors

import play.api.libs.json._
import WSId._
import WsData._
import WsMessageJson._
import WsActor.SenderEnvelope

trait ChatActor extends WsActor {
  def name: String

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[SenderEnvelope])
    super.preStart()
  }

  def receiveChat: Receive = {
    case SenderEnvelope(sender, WsMessage(CHAT, _)) if sender == self =>
    case SenderEnvelope(_, message @ WsMessage(CHAT, _)) =>
      sendToWS(message)
  }

  override def handleWsMessage(id: WSId, data: JsObject): Unit = id match {
    case CHAT => processData[Chat](id, data) { chat =>
      context.system.eventStream.publish(SenderEnvelope(self, WsMessage(CHAT, chat)))
    }
    case _ => super.handleWsMessage(id, data)
  }
}

