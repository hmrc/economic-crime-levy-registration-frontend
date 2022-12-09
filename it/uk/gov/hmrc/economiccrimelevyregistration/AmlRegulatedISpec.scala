package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class AmlRegulatedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlRegulatedController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedController.onPageLoad())

    "respond with 200 status and the Aml regulated HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val expectedTaxYear = EclTaxYear.currentFinancialYear

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlRegulatedController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Did you start AML-regulated activity in FY $expectedTaxYear?")
    }
  }

  s"POST ${routes.AmlRegulatedController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.AmlRegulatedController.onSubmit())

    "save the selected Aml regulated option then redirect to the SIC code page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(startedAmlRegulatedActivity = Some(false))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedController.onSubmit()).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe ???
    }

    "save the selected Aml regulated option then redirect to the Aml start date page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(startedAmlRegulatedActivity = Some(true))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedController.onSubmit()).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(AmlStartDateController.onPageLoad().url)
    }
  }
}
