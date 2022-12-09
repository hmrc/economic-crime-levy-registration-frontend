package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models._

class AmlRegulatedActivityStartDateISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlRegulatedActivityStartDateController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityStartDateController.onPageLoad())

    "respond with 200 status and the Aml start date HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityStartDateController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("What date did your AML-regulated activity start?")
    }
  }
}
