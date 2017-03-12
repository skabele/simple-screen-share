package skabele.screenshare.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import play.api.libs.json._

object WsActor {
  abstract class WsMessage(val msg: String)

  case class Error(override val msg: String, text: String) extends WsMessage(msg)
  object Error {
    val msg = "ERROR"
    implicit val format = Json.format[Error]
  }
}

trait WsActor extends Actor with ActorLogging {

  import WsActor.Error
  def socket: ActorRef

  def receiveJson: Receive = {
    case json: JsValue =>
      (json \ "msg").toOption match {
        case Some(JsString(msg)) => handleWsMessage(msg, json)
        case _ => send(Error(Error.msg, "Message does not contain required string field 'msg'"))
      }
  }

  def send[T: Writes](message: T): Unit = {
    log.debug(s"Sending to ws: $message")
    socket ! Json.toJson(message)
  }

  def sendError(text: String): Unit = send(Error(Error.msg, text))

  def validationError(json: JsValue, e: JsError): Unit =
    sendError(s"Unable to validate message: $json errors: " + JsError.toFlatForm(e))

  def handleWsMessage(msg: String, json: JsValue): Unit = msg match {
    case Error.msg =>
      json.validate[Error] match {
        case s: JsSuccess[Error] =>
          log.warning("Error reported over WS: " + s.get.text)
        case e: JsError => validationError(json, e)
      }
    case other => sendError(s"Unknown message type msg='$other'")
  }

}
