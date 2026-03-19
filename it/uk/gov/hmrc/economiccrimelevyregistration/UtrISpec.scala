package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.utrLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class UtrISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.UtrController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrController.onPageLoad(mode))

      "respond with 200 status and the company registration number HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubGetRegistrationAdditionalInfo(
          RegistrationAdditionalInfo.apply(
            registration.internalId,
            None,
            None,
            None,
            None,
            None
          )
        )
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.UtrController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is your unique taxpayer reference?")
      }
    }

    s"POST ${routes.UtrController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrController.onSubmit(mode))

      "save the UTR then redirect to the company registration number page page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = arbitrary[Registration].sample.get
          .copy(entityType = Some(arbitrary[EntityType].sample.get), relevantApRevenue = Some(randomApRevenue()))
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get
        val Utr            = numStringsWithConcreteLength(utrLength).sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val otherEntityJourneyData = registration.otherEntityJourneyData.copy(ctUtr = Some(Utr), saUtr = None)
        val updatedRegistration    = registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.UtrController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", Utr))
        )

        status(result) shouldBe SEE_OTHER

        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              routes.CompanyRegistrationNumberController.onPageLoad(mode).url
            )
          case CheckMode  =>
            if (updatedRegistration.otherEntityJourneyData.companyRegistrationNumber.isEmpty) {
              redirectLocation(result) shouldBe Some(
                routes.CompanyRegistrationNumberController.onPageLoad(mode).url
              )
            } else {
              redirectLocation(result) shouldBe Some(
                routes.CheckYourAnswersController.onPageLoad().url
              )
            }

        }
      }

    }
  }

}
