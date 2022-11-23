package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models._

class AmlSupervisorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlSupervisorController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlSupervisorController.onPageLoad())

    "respond with 200 status and the AML supervisor HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlSupervisorController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Who is your AML supervisor?")
    }
  }

}
