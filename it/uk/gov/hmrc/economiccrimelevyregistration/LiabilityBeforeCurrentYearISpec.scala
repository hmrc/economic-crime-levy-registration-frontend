package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class LiabilityBeforeCurrentYearISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the liability before current year HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Were you liable to pay the ECL from 1 April 2022 to 31 March 2023?")
    }
  }

  s"POST ${routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode)
    )

    "save the selected address option then redirect to the address lookup frontend journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = random[Boolean]
      val registration            = random[Registration].copy(
        registrationType = Some(Initial)
      )
      val additionalInfo          = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val info = RegistrationAdditionalInfo(
        registration.internalId,
        None,
        None
      )

      stubUpsertRegistrationAdditionalInfo(info)
      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }
  }
}
