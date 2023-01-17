package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class RelevantAp12MonthsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RelevantAp12MonthsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onPageLoad())

    "respond with 200 status and the relevant AP 12 months view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.RelevantAp12MonthsController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Is your relevant accounting period 12 months?")
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onSubmit())

    "save the selected option then redirect to the UK revenue page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(relevantAp12Months = Some(true))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit()).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad().url)
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(relevantAp12Months = Some(false))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit()).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantApLengthController.onPageLoad().url)
    }
  }
}
