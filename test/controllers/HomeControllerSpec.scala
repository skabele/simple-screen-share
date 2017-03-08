package controllers

import org.scalatestplus.play._
import play.api.mvc.AnyContentAsEmpty
import play.api.test._
import play.api.test.Helpers._

class HomeControllerSpec extends PlaySpec with OneAppPerTest {

  "HomeController GET" should {

    "render the index page for Provider when requesting from localhost" in {
      val result = route(app, FakeRequest(GET, "/")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include ("Let's share desktop")
    }

    "render the index page for Master when requesting from other host" in {
      val result = route(app, FakeRequest(GET, "/", FakeHeaders(), AnyContentAsEmpty, remoteAddress = "some.io")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include ("Let's view shared desktop")
    }

  }
}
