package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CancelRegistrationAmendmentISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CancelRegistrationAmendmentController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CancelRegistrationAmendmentController.onPageLoad()
    )

    "respond with 200 status and the cancel registration view" in {
      stubAuthorisedWithEclEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
        .copy(registrationType = Some(Amendment))
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
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
      stubAuthorisedWithEclEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          registrationType = Some(Amendment),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubDeleteRegistration()

      val result = callRoute(
        FakeRequest(routes.CancelRegistrationAmendmentController.onSubmit())
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(appConfig.yourEclAccountUrl)
    }

    "return to the Check Your Answers Page when the No option is selected" in {
      stubAuthorisedWithEclEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          registrationType = Some(Amendment),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      val result = callRoute(
        FakeRequest(routes.CancelRegistrationAmendmentController.onSubmit())
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad().url
      )
    }
  }
}
