package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CancelRegistrationAmendmentISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CancelRegistrationAmendmentController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CancelRegistrationAmendmentController.onPageLoad()
    )

    "respond with 200 status and the cancel registration view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistration(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      val result = callRoute(FakeRequest(routes.CancelRegistrationAmendmentController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Are you sure you want to cancel your request to amend your registration?")
    }
  }

  s"POST ${routes.CancelRegistrationAmendmentController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CancelRegistrationAmendmentController.onSubmit()
    )

    "delete the registration when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)
      stubDeleteRegistration()

      val result = callRoute(
        FakeRequest(routes.CancelRegistrationAmendmentController.onSubmit())
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(appConfig.yourEclAccountUrl)
    }
  }
}
