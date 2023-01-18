package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class AmlRegulatedActivityISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlRegulatedActivityController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityController.onPageLoad())

    "respond with 200 status and the Aml regulated HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val expectedTaxYearStart = EclTaxYear.currentFyStartYear
      val expectedTaxYearEnd   = EclTaxYear.currentFyEndYear

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(
        s"Did you carry out AML-regulated activity between 1 April $expectedTaxYearStart and 31 March $expectedTaxYearEnd?"
      )
    }
  }

  s"POST ${routes.AmlRegulatedActivityController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityController.onSubmit())

    "save the selected AML regulated activity option then redirect to the AML supervisor page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit()).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlSupervisorController.onPageLoad().url)
    }

    "save the selected AML regulated activity option then redirect to the not liable page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(false))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit()).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotLiableController.onPageLoad().url)
    }
  }
}
