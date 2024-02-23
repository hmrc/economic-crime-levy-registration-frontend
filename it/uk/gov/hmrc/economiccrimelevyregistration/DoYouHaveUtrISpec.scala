package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
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
      stubSessionForStoreUrl(registration.internalId, routes.DoYouHaveUtrController.onPageLoad(NormalMode))

      val result = callRoute(FakeRequest(routes.DoYouHaveUtrController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Do you have a Unique Taxpayer Reference?")
    }
  }

  s"POST ${routes.DoYouHaveUtrController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.DoYouHaveUtrController.onSubmit(NormalMode))

    "save the selected option" in {
      stubAuthorisedWithNoGroupEnrolment()

      val hasUtr       = random[Boolean]
      val registration = random[Registration]

      val otherEntityJourneyData = registration.otherEntityJourneyData.copy(
        isCtUtrPresent = Some(hasUtr),
        ctUtr = hasUtr match {
          case true  => registration.otherEntityJourneyData.ctUtr
          case false => None
        }
      )
      val updatedRegistration    = registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
      val additionalInfo         = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.DoYouHaveUtrController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", hasUtr.toString))
      )

      status(result) shouldBe SEE_OTHER
    }
  }
}
