package skabele.screenshare.actors

import akka.actor._
import WsId._
import WsData._

trait WsActor extends Actor with ActorLogging {
  def socket: ActorRef
  var name: String

  def sendErrorToWS(text: String): Unit = socket ! WsMessage(SERVER_ERROR, Error(text))

  def logWarnAndSendErrorToWS(text: String): Unit = {
    log.warning(text)
    sendErrorToWS(text)
  }

  def receiveError: Receive = {
    case WsMessage(FRONTEND_ERROR, Error(text)) =>
      log.warning(s"Screen or client name=$name reported error over WS: $text")
    case WsMessage(PARSE_ERROR, Error(text)) =>
      logWarnAndSendErrorToWS(s"Received message is valid JSON, but none of recognised messages")
    case unexpected =>
      log.error(s"Logical error on ${self.path} - unhandled message $unexpected")
  }
}
