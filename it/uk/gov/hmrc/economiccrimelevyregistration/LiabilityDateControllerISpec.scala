package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, NormalMode, Registration, RegistrationAdditionalInfo}

import java.time.LocalDate

class LiabilityDateControllerISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.LiabilityDateController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityDateController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the liability date HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.LiabilityDateController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Enter the date you became liable for ECL")
    }
    s"POST ${routes.LiabilityDateController.onSubmit(CheckMode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.LiabilityDateController.onSubmit(CheckMode)
      )
      "save the entered date then redirect to the check your answers page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val date = LocalDate.now()

        val registration = random[Registration].copy(
          registrationType = Some(Initial)
        )

        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistration(registration)
        val info = RegistrationAdditionalInfo(
          testInternalId,
          None,
          Some(date),
          None,
          None,
          None
        )

        stubUpsertRegistrationAdditionalInfo(info)
        stubUpsertRegistration(registration)

        val result = callRoute(
          FakeRequest(routes.LiabilityDateController.onSubmit(CheckMode))
            .withFormUrlEncodedBody(
              ("value.day", date.getDayOfMonth.toString),
              ("value.month", date.getMonthValue.toString),
              ("value.year", date.getYear.toString)
            )
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
      }

    }
  }
}
