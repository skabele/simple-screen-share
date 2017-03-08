package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import org.slf4j.LoggerFactory

@Singleton
class HomeController @Inject() extends Controller {

  private val logger = LoggerFactory.getLogger(getClass)

  def index = Action { implicit request =>
    if (request.remoteAddress == "127.0.0.1") {
      Ok(views.html.provider())
    } else {
      Ok(views.html.client())
    }
  }
}
