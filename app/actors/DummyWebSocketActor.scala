package actors

import akka.actor._

object DummyWebSocketActor {
  def props(name: String, out: ActorRef) = Props(new DummyWebSocketActor(name, out))
}

class DummyWebSocketActor(name: String, out: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case msg: String =>
      log.info(s"$name received: $msg")
      out ! (s"I, $name, received your message: " + msg)
  }

  override def postStop() = {
    log.info(s"WS actor $name stopped")
  }
}