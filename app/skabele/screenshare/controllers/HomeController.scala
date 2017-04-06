package skabele.screenshare.controllers

import javax.inject._
import skabele.screenshare.views
import play.api.mvc._
import skabele.screenshare.WithLogger

@Singleton
class HomeController @Inject() extends Controller with WithLogger {

  def index = Action { implicit request =>
    if (request.remoteAddress == "127.0.0.1" || request.remoteAddress == "0:0:0:0:0:0:0:1") {
      Ok(views.html.sharedDesktop())
    } else {
      Ok(views.html.client())
    }
  }
}
