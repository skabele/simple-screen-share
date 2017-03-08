package controllers

import actors.DummyWebSocketActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import helpers.WithLogger
import play.api.mvc._
import play.api.libs.streams._

class WebSocketController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends WithLogger {

  def providerSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => DummyWebSocketActor.props("Provider", out))
  }

  def clientSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => DummyWebSocketActor.props("Client", out))
  }
}
