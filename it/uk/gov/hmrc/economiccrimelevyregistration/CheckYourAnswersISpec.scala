package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors

class CheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CheckYourAnswersController.onPageLoad())

    "respond with 200 status and the Check your answers HTML view when the registration data is valid" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val errors       = random[DataValidationErrors]

      stubGetRegistration(registration)
      stubGetRegistrationValidationErrors(valid = true, errors)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Check your answers")
    }

    "redirect to the journey recovery page when the registration data is invalid" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val errors       = random[DataValidationErrors]

      stubGetRegistration(registration)
      stubGetRegistrationValidationErrors(valid = false, errors)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onPageLoad()))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)
    }
  }

  s"POST ${routes.CheckYourAnswersController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CheckYourAnswersController.onSubmit())

    "redirect to the registration submitted page after submitting the registration successfully" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val eclReference = random[String]

      stubGetRegistration(registration)

      stubSubmitRegistration(eclReference)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onSubmit()))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.RegistrationSubmittedController.onPageLoad().url)
    }
  }

}
