package skabele.screenshare.controllers

import org.scalatestplus.play._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test._

class HomeControllerSpec extends PlaySpec with OneAppPerTest {

  "HomeController GET" should {

    "render the index page for Provider when requesting from localhost" in {
      val result = route(app, FakeRequest(GET, "/")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include ("Simple Screen Share")
    }

    "render the index page for Master when requesting from other host" in {
      val result = route(app, FakeRequest(GET, "/", FakeHeaders(), AnyContentAsEmpty, remoteAddress = "some.io")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include ("View controlls")
    }

  }
}
