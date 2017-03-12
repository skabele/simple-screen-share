package skabele.screenshare.actors

import akka.actor.ActorRef
import play.api.libs.json._

object ChatActor {

  case class ChatMessage(override val msg: String, text: String) extends WsActor.WsMessage(msg)
  object ChatMessage {
    val msg = "CHAT"
    implicit val format = Json.format[ChatMessage]
  }

  case class ChatEnvelope(sender: ActorRef, message: WsActor.WsMessage)
}

trait ChatActor extends WsActor {

  import ChatActor._

  def name: String

  override def preStart(): Unit = {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[ChatEnvelope])
  }

  def receiveChat: Receive = {
    case ChatEnvelope(sender, ChatMessage(msg, text)) if sender == self =>
    case ChatEnvelope(_, message: ChatMessage) =>
      send(message)

  }

  override def handleWsMessage(msg: String, json: JsValue): Unit = msg match {
    case ChatMessage.msg =>
      json.validate[ChatMessage] match {
        case s: JsSuccess[ChatMessage] =>
          context.system.eventStream.publish(ChatEnvelope(self, s.get))
        case e: JsError => validationError(json, e)
      }
    case _ => super.handleWsMessage(msg, json)
  }
}

