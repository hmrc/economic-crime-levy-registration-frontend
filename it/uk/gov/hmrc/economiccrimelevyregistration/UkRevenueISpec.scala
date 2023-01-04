package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes._
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.UkRevenueController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad())

    "respond with 200 status and the uk revenue HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val expectedTaxYear = EclTaxYear.currentFinancialYear

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"What was your UK revenue in $expectedTaxYear?")
    }
  }

  s"POST ${routes.UkRevenueController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.UkRevenueController.onSubmit())

    "save the selected Uk revenue option then redirect to the 'you do not need to register' page when the LessThan option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(false))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit()).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(NotLiableController.onPageLoad().url)
    }

    "save the selected Uk revenue option then redirect to the 'who is your AML Supervisor?' page when the EqualToOrGreaterThan option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(true))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit()).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(AmlSupervisorController.onPageLoad().url)
    }
  }
}
