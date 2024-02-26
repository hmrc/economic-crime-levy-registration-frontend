package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Trust
import uk.gov.hmrc.economiccrimelevyregistration.models._

class DoYouHaveUtrISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.DoYouHaveUtrController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.DoYouHaveUtrController.onPageLoad(NormalMode))

    "respond with 200 status and the do you have utr HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.DoYouHaveUtrController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Do you have a Unique Taxpayer Reference?")
    }
  }

  s"POST ${routes.DoYouHaveUtrController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.DoYouHaveUtrController.onSubmit(NormalMode))

    "save the selected option" in {
      stubAuthorisedWithNoGroupEnrolment()

      val hasUtr            = random[Boolean]
      val registration      = random[Registration]
      val validRegistration = registration.copy(entityType = Some(Trust))

      val otherEntityJourneyData = validRegistration.otherEntityJourneyData.copy(
        isCtUtrPresent = Some(hasUtr),
        ctUtr = if (hasUtr) {
          validRegistration.otherEntityJourneyData.ctUtr
        } else {
          None
        }
      )
      val updatedRegistration    = validRegistration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
      val additionalInfo         = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(validRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.DoYouHaveUtrController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", hasUtr.toString))
      )

      status(result) shouldBe SEE_OTHER
    }
  }
}
