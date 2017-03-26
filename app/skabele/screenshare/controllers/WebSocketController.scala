package skabele.screenshare.controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import play.api.libs.json._
import play.api.libs.streams._
import play.api.mvc._
import skabele.screenshare.WithLogger
import skabele.screenshare.actors.{ClientActor, ScreenShareActor, WsMessage}
import skabele.screenshare.actors.WsMessageJson._
import play.api.mvc.WebSocket.MessageFlowTransformer
import skabele.screenshare.actors.WsData._
import skabele.screenshare.actors.WsId._

class WebSocketController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends WithLogger {

  def flowTransformer: MessageFlowTransformer[WsMessage, WsMessage] =
    MessageFlowTransformer.jsonMessageFlowTransformer.map(
      json => {
        logger.trace("IN: " + json.toString)
        Json.fromJson[WsMessage](json).fold({ errors =>
          logger.warn(s"Received message ${json.toString} is valid JSON, but none of recognised messages")
          WsMessage(PARSE_ERROR, Error(json.toString))
        }, identity)
      },
      out => {
        logger.trace("OUT: " + out.toString)
        Json.toJson[WsMessage](out)
      }
    )

  def sharedDesktopSocket =
    WebSocket.accept[WsMessage, WsMessage] { request =>
      request.uri
      logger.info("Screen connected to websocket")
      ActorFlow.actorRef(out => ScreenShareActor.props(out))
    } (flowTransformer)

  def clientSocket =
    WebSocket.accept[WsMessage, WsMessage] { request =>
      logger.info(s"Client ${request.remoteAddress} connected to websocket")
      ActorFlow.actorRef(out => ClientActor.props(out))
    } (flowTransformer)
}
