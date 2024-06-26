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

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.DoYouHaveUtrController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.DoYouHaveUtrController.onPageLoad(mode))

      "respond with 200 status and the do you have utr HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.DoYouHaveUtrController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Do you have a Unique Taxpayer Reference?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${routes.DoYouHaveUtrController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.DoYouHaveUtrController.onSubmit(mode))

      "save the selected option" in {
        stubAuthorisedWithNoGroupEnrolment()

        val hasUtr            = random[Boolean]
        val registration      = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
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
        stubGetRegistrationWithEmptyAdditionalInfo(validRegistration)
        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.DoYouHaveUtrController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", hasUtr.toString))
        )

        status(result) shouldBe SEE_OTHER
      }
    }
  }
}
