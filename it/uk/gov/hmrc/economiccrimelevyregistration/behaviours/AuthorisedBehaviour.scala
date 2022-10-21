package uk.gov.hmrc.economiccrimelevyregistration.behaviours

import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase

import scala.concurrent.Future

trait AuthorisedBehaviour {
  self: ISpecBase =>

  def authorisedActionRoute(call: Call): Unit =
    "go to already registered page if the user has the ECL enrolment" in {
      stubAuthorisedWithEclEnrolment()

      val result: Future[Result] = callRoute(FakeRequest(call))

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Already registered - user already has enrolment"
    }

}
