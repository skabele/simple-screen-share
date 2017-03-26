package skabele.screenshare.actors

import WsId._
import WsData._
import InternalMessage._

trait ChatActor extends WsActor {

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[ChatPublished])
    super.preStart()
  }

  def receiveChat: Receive = {
    case WsMessage(SEND_CHAT, SendChat(text)) =>
      context.system.eventStream.publish(ChatPublished(self, name, text))

    case ChatPublished(sender, _, _) if sender == self =>
    case ChatPublished(_, _name, text) =>
      socket ! WsMessage(CHAT_MSG, ChatMsg(_name, text))
  }
}

