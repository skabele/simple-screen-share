package skabele.screenshare.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object ScreenShareActor {
  def props(name: String, socket: ActorRef) = Props(new ScreenShareActor(name, socket))
}

class ScreenShareActor(override val name: String, override val socket: ActorRef)
  extends Actor with ActorLogging with ChatActor {

  override def receive = receiveJson orElse receiveChat
}