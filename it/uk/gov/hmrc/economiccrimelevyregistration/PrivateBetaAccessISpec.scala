package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class PrivateBetaAccessISpec extends ISpecBase with AuthorisedBehaviour {

  val testContinueUrl = "/test-continue-url"

  s"GET ${routes.PrivateBetaAccessController.onPageLoad(testContinueUrl).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.PrivateBetaAccessController.onPageLoad(testContinueUrl)
    )

    "respond with 200 status and the private beta access HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val result = callRoute(FakeRequest(routes.PrivateBetaAccessController.onPageLoad(testContinueUrl)))

      status(result) shouldBe OK

      html(result) should include("Register for the Economic Crime Levy")
    }
  }

  s"POST ${routes.PrivateBetaAccessController.onSubmit(testContinueUrl).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.PrivateBetaAccessController.onSubmit(testContinueUrl))

    "save the access code and redirect to the continue URL if the access code matches the one held in config" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val accessCode   = "123456"

      stubUpsertRegistration(Registration.empty(testInternalId))

      val updatedRegistration = registration.copy(privateBetaAccessCode = Some(accessCode))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.PrivateBetaAccessController.onSubmit(testContinueUrl))
          .withFormUrlEncodedBody(("value", accessCode))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(testContinueUrl)
    }

    "display a form error if the access code does not match the one held in config" in {
      stubAuthorisedWithNoGroupEnrolment()

      val accessCode = "111111"

      val result = callRoute(
        FakeRequest(routes.PrivateBetaAccessController.onSubmit(testContinueUrl))
          .withFormUrlEncodedBody(("value", accessCode))
      )

      status(result) shouldBe BAD_REQUEST

      html(result) should include("Enter a valid access code")
    }
  }
}
