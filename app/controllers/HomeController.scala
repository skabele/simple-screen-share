package controllers

import javax.inject._

import helpers.WithLogger
import play.api._
import play.api.mvc._

@Singleton
class HomeController @Inject() extends Controller with WithLogger {

  def index = Action { implicit request =>
    if (request.remoteAddress == "127.0.0.1") {
      Ok(views.html.sharedDesktop())
    } else {
      Ok(views.html.client())
    }
  }
}
