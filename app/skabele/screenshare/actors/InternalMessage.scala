package skabele.screenshare.actors

import akka.actor.ActorRef
import WsData._

object InternalMessage {
  case class ChatPublished(sender: ActorRef, name: String, text: String)
  case class ClientReadyToListen(ref: ActorRef, name: String, isResponse: Boolean)
  case class ScreenReadyToShare(ref: ActorRef, name: String, isResponse: Boolean)
  case class NameAlreadyTaken()
  case class ScreenAlreadyConnected()
  case class IceCandidate(candidate: RTCIceCandidate)
  case class Session(session: RTCSessionDescription)
}
