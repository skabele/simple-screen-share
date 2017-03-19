package skabele.screenshare.actors

import akka.actor._
import play.api.libs.json._
import WSId._
import WsData._
import WsMessageJson._

object WsActor {
  case class SenderEnvelope(sender: ActorRef, message: WsMessage)
}

trait WsActor extends Actor with ActorLogging {

  def socket: ActorRef

  def receiveJson: Receive = {
    case json: JsValue =>
      ((json \ "id").toOption, (json \ "data").toOption) match {
        case (Some(JsString(id)), Some(data: JsObject)) =>
          handleWsMessage(id, data)
        case (Some(JsString(id)), None) =>
          handleWsMessage(id, JsObject(Seq()))
        case _ =>
          sendErrorToWS(s"Message $json does not contains string field 'id' or (optional) 'data' field is not object")
      }
  }

  private def handleWsMessage(id: String, data: JsObject): Unit =
    WSId.withNameOption(id) match {
      case None => sendErrorToWS(s"Message has 'id' with invalid value '$id'")
      case Some(wsId) => handleWsMessage(wsId, data)
    }

  def handleWsMessage(id: WSId, data: JsObject): Unit = id match {
    case ERROR => processData[Error](id, data) {
        error => log.warning("Error reported over WS: " + error)
      }
    case other => sendErrorToWS(s"Unprocessed message id=$id data=$data (maybe send to wrong socket?)")
  }

  def sendToWS(message: WsMessage): Unit = {
    log.debug(s"Sending to ws: $message")
    socket ! Json.toJson(message)
  }

  def sendErrorToWS(text: String): Unit = sendToWS(WsMessage(ERROR, Error(text)))

  def processEmptyData(id: WSId, data: JsObject)(process: () => Unit): Unit =
    if (data.value.isEmpty) {
      process()
    } else {
      sendErrorToWS(s"Unable to validate id=$id data=$data - expecting empty data")
    }

  def processData[T : Reads](id: WSId, data: JsObject)(process: T => Unit): Unit =
    data.validate[T] match {
      case success: JsSuccess[T] =>
        process(success.get)
      case error: JsError =>
        sendErrorToWS(s"Unable to validate id=$id data=$data - errors: " + JsError.toFlatForm(error))
    }
}
