package skabele.screenshare.actors

import enumeratum._
import play.api.libs.json._

sealed trait WSId extends EnumEntry
object WSId extends Enum[WSId] with PlayJsonEnum[WSId] {
  val values = findValues

  case object ERROR extends WSId
  case object CHAT extends WSId
  case object CLIENT_READY extends WSId
  case object SCREEN_READY extends WSId
  case object RTC_ICE_CANDIDATE extends WSId
  case object RTC_SESSION_DESCRIPTION extends WSId
}

sealed trait WsData
object WsData {
  case class NoData() extends WsData
  case class Error(text: String) extends WsData
  case class Chat(text: String) extends WsData
  case class RTCIceCandidate(candidate: String, sdpMLineIndex: Int, sdpMid: String ) extends WsData
  case class RTCSessionDescription(sdp: String, `type`: String) extends WsData
}

case class WsMessage(id: WSId, data: WsData = WsData.NoData())

object WsMessageJson {
  import WsData._

  implicit val writesNoData = new Writes[NoData] {
    def writes(person: NoData) = Json.obj()
  }
  implicit val formatError = Json.format[Error]
  implicit val formatChat = Json.format[Chat]
  implicit val formatRTCIceCandidate = Json.format[RTCIceCandidate]
  implicit val formatRTCSessionDescription = Json.format[RTCSessionDescription]

  implicit val writesWsData = Writes[WsData] {
    case data: NoData => writesNoData.writes(data)
    case data: Error => formatError.writes(data)
    case data: Chat => formatChat.writes(data)
    case data: RTCIceCandidate => formatRTCIceCandidate.writes(data)
    case data: RTCSessionDescription => formatRTCSessionDescription.writes(data)
  }

  implicit val writesWsMessage = Json.writes[WsMessage]
}