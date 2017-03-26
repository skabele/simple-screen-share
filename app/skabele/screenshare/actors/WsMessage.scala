package skabele.screenshare.actors

import enumeratum._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

sealed trait WsId extends EnumEntry
object WsId extends Enum[WsId] with PlayJsonEnum[WsId] {
  val values = findValues

  case object FRONTEND_ERROR extends WsId
  case object SERVER_ERROR extends WsId
  case object PARSE_ERROR extends WsId

  case object SEND_CHAT extends WsId
  case object CHAT_MSG extends WsId
  case object CLIENT_READY extends WsId
  case object CLIENT_LEFT extends WsId
  case object SCREEN_READY extends WsId
  case object SCREEN_LEFT extends WsId
  case object NAME_ALREADY_TAKEN extends WsId
  case object SCREEN_ALREADY_CONNECTED extends WsId
  case object RTC_ICE_CANDIDATE extends WsId
  case object RTC_ICE_CANDIDATE_WITH_NAME extends WsId
  case object RTC_SESSION_DESCRIPTION extends WsId
  case object RTC_SESSION_DESCRIPTION_WITH_NAME extends WsId
}

sealed trait WsData
object WsData {
  case class NoData() extends WsData
  case class Error(text: String) extends WsData
  case class SendChat(text: String) extends WsData
  case class ChatMsg(name: String, text: String) extends WsData
  case class ScreenReady(name: String) extends WsData
  case class ClientReady(clientName: String) extends WsData
  case class ClientLeft(clientName: String) extends WsData
  case class RTCIceCandidate(candidate: String, sdpMLineIndex: Int, sdpMid: String) extends WsData
  case class RTCIceCandidateWithName(clientName: String, candidate: RTCIceCandidate) extends WsData
  case class RTCSessionDescription(sdp: String, `type`: String) extends WsData
  case class RTCSessionDescriptionWithName(clientName: String, session: RTCSessionDescription) extends WsData
}

case class WsMessage(id: WsId, data: WsData)

object WsMessageJson {
  import WsData._
  import WsId._


  implicit val writesNoData = new Writes[NoData] {
    def writes(person: NoData) = Json.obj()
  }
  implicit val readsNoData = new Reads[NoData] {
    def reads(js: JsValue): JsResult[NoData] = js match {
      case _: JsObject => JsSuccess(NoData())
      case _ => JsError("Expecting JSON object")
    }
  }
  implicit val formatError = Json.format[Error]
  implicit val formatSendChat = Json.format[SendChat]
  implicit val formatChatMsg = Json.format[ChatMsg]
  implicit val formatScreenReady = Json.format[ScreenReady]
  implicit val formatClientReady = Json.format[ClientReady]
  implicit val formatClientLeft = Json.format[ClientLeft]
  implicit val formatRTCIceCandidate = Json.format[RTCIceCandidate]
  implicit val formatRTCIceCandidateWithName = Json.format[RTCIceCandidateWithName]
  implicit val formatRTCSessionDescription = Json.format[RTCSessionDescription]
  implicit val formatRTCSessionDescriptionWithName = Json.format[RTCSessionDescriptionWithName]

  implicit val writesWsData = Writes[WsData] {
    case data: NoData => writesNoData.writes(data)
    case data: Error => formatError.writes(data)
    case data: SendChat => formatSendChat.writes(data)
    case data: ChatMsg => formatChatMsg.writes(data)
    case data: ScreenReady => formatScreenReady.writes(data)
    case data: ClientReady => formatClientReady.writes(data)
    case data: ClientLeft => formatClientLeft.writes(data)
    case data: RTCIceCandidate => formatRTCIceCandidate.writes(data)
    case data: RTCIceCandidateWithName => formatRTCIceCandidateWithName.writes(data)
    case data: RTCSessionDescription => formatRTCSessionDescription.writes(data)
    case data: RTCSessionDescriptionWithName => formatRTCSessionDescriptionWithName.writes(data)
  }

  implicit val writesWsMessage = Json.writes[WsMessage]

  def equals[N](expected: N)(implicit reads: Reads[N]) =
    filter[N](ValidationError("error.is", expected))(_ == expected)(reads)

  def readMessage[Data <: WsData :Reads](wsid: WsId): Reads[WsMessage] = (
    (JsPath \ "id").read[WsId](equals(wsid)) and
    (JsPath \ "data").read[Data]
  )(WsMessage.apply _)

  implicit val readsWsData: Reads[WsMessage] =
    readMessage[Error](SERVER_ERROR) orElse
    readMessage[Error](PARSE_ERROR) orElse
    readMessage[SendChat](SEND_CHAT) orElse
    readMessage[ChatMsg](CHAT_MSG) orElse
    readMessage[ClientReady](CLIENT_READY) orElse
    readMessage[ClientLeft](CLIENT_LEFT) orElse
    readMessage[ScreenReady](SCREEN_READY) orElse
    readMessage[NoData](SCREEN_LEFT) orElse
    readMessage[NoData](NAME_ALREADY_TAKEN) orElse
    readMessage[NoData](SCREEN_ALREADY_CONNECTED) orElse
    readMessage[RTCIceCandidate](RTC_ICE_CANDIDATE) orElse
    readMessage[RTCIceCandidateWithName](RTC_ICE_CANDIDATE_WITH_NAME) orElse
    readMessage[RTCSessionDescription](RTC_SESSION_DESCRIPTION) orElse
    readMessage[RTCSessionDescriptionWithName](RTC_SESSION_DESCRIPTION_WITH_NAME)
}