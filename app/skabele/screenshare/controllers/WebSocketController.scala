package skabele.screenshare.controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.streams._
import play.api.mvc._
import skabele.screenshare.WithLogger
import skabele.screenshare.actors.ChatActor

class WebSocketController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends WithLogger {

  def sharedDesktopSocket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => ChatActor.props("Shared desktop", out))
  }

  def clientSocket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => ChatActor.props("Client", out))
  }
}
