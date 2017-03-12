package skabele.screenshare.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object ClientActor {
  def props(name: String, socket: ActorRef) = Props(new ClientActor(name, socket))
}

class ClientActor(override val name: String, override val socket: ActorRef)
  extends Actor with ActorLogging with ChatActor {

  override def receive = receiveJson orElse receiveChat
}
