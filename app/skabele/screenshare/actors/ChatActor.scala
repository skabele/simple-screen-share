package skabele.screenshare.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.libs.json.{JsLookupResult, _}

object ChatActor {
  def props(name: String, socket: ActorRef) = Props(new ChatActor(name, socket))

  sealed abstract class ExternalMessage(val msg: String)

  case class Ready(override val msg: String) extends ExternalMessage(msg)
  object Ready {
    val msg = "READY"
    implicit val format = Json.format[Ready]
  }

  case class Bye(override val msg: String) extends ExternalMessage(msg)
  object Bye {
    val msg = "BYE"
    implicit val format = Json.format[Bye]
  }

  case class ChatMessage(override val msg: String, text: String) extends ExternalMessage(msg)
  object ChatMessage {
    val msg = "CHAT"
    implicit val format = Json.format[ChatMessage]
  }

  case class Error(override val msg: String, text: String) extends ExternalMessage(msg)
  object Error {
    val msg = "ERROR"
    implicit val format = Json.format[Error]
  }

  case class ChatEnvelope(sender: ActorRef, message: ExternalMessage)
}

class ChatActor(name: String, socket: ActorRef) extends Actor with ActorLogging {
  import ChatActor._

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[ChatEnvelope])

  def receive = {
    case ChatEnvelope(sender, ChatMessage(msg, text)) if sender == self =>
    case ChatEnvelope(_, message: ChatMessage) =>
      send(message)
    case json: JsValue =>
      (json \ "msg").toOption match {
        case Some(JsString(msg)) => handleExternalMessage(msg, json)
        case _ => send(Error(Error.msg, "Message does not contain required string field 'msg'"))
      }
  }

  def handleExternalMessage(msg: String, json: JsValue): Unit = {
    def report(e: JsError): Unit =
      sendError(s"Invalid format of message msg=$msg : " + JsError.toFlatForm(e))

    msg match {
      case Ready.msg =>
      case Bye.msg =>
      case ChatMessage.msg =>
        json.validate[ChatMessage] match {
          case s: JsSuccess[ChatMessage] =>
            context.system.eventStream.publish(ChatEnvelope(self, s.get))
          case e: JsError => report(e)
        }
      case Error.msg =>
        json.validate[Error] match {
          case s: JsSuccess[Error] =>
            log.warning("Error reported over WS: " + s.get.text)
          case e: JsError =>
        }
      case other => sendError(s"Unknown message type msg='$other'")
    }
  }

  override def postStop(): Unit = log.debug(s"Actor $getClass name=$name stopped")

  def send[T: Writes](message: T): Unit = {
    log.debug(s"Sending to ws: $message")
    socket ! Json.toJson(message)
  }

  def sendError(text: String): Unit = send(Error(Error.msg, text))
}