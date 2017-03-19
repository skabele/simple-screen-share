package skabele.screenshare.controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.streams._
import play.api.mvc._
import skabele.screenshare.WithLogger
import skabele.screenshare.actors.{ClientActor, ScreenShareActor}

class WebSocketController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends WithLogger {

  def sharedDesktopSocket = WebSocket.accept[JsValue, JsValue] { request =>
    logger.info("initialazing Screen Share")
    ActorFlow.actorRef(out => ScreenShareActor.props("Shared desktop", out))
  }

  def clientSocket = WebSocket.accept[JsValue, JsValue] { request =>
    logger.info("initialazing Client")
    ActorFlow.actorRef(out => ClientActor.props("Client", out))
  }
}
