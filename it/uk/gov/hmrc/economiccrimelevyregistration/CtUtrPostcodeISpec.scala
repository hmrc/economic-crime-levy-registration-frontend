package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.ctUtrPostcodeLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.UnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CtUtrPostcodeISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.CtUtrPostcodeController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.CtUtrPostcodeController.onPageLoad(mode)
      )

      "respond with 200 status and the CtUtrPostcode HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val postcode: String = stringsWithMaxLength(ctUtrPostcodeLength).sample.get

        val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(postcode = Some(postcode))

        val registration: Registration = random[Registration]
          .copy(
            entityType = Some(UnincorporatedAssociation),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        val additionalInfo: RegistrationAdditionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.CtUtrPostcodeController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is the postcode you used to register your association?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${routes.CtUtrPostcodeController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.CtUtrPostcodeController.onSubmit(mode))

      "save the postcode then redirect to the business sector controller page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(UnincorporatedAssociation),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        val postcode = stringsWithMaxLength(ctUtrPostcodeLength).sample.get

        val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(companyRegistrationNumber = Some(postcode))
        val updatedRegistration    = registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubGetRegistrationAdditionalInfo(additionalInfo)

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.CtUtrPostcodeController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", postcode))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
          case CheckMode  =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad().url
            )
        }

      }
    }
  }
}
