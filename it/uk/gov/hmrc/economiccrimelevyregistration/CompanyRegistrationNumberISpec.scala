package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.companyRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CompanyRegistrationNumberISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.CompanyRegistrationNumberController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.CompanyRegistrationNumberController.onPageLoad(mode)
      )

      "respond with 200 status and the company registration number HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val companyNumber: String = stringsWithMaxLength(companyRegistrationNumberMaxLength).sample.get

        val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(companyRegistrationNumber = Some(companyNumber))

        val registration: Registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            optOtherEntityJourneyData = Some(otherEntityJourneyData),
            relevantApRevenue = Some(randomApRevenue())
          )

        val additionalInfo: RegistrationAdditionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.CompanyRegistrationNumberController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is your company registration number?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${routes.CompanyRegistrationNumberController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.CompanyRegistrationNumberController.onSubmit(NormalMode)
      )

      "save the company registration number then redirect to the Business Sector Controller page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        val companyNumber = stringsWithMaxLength(companyRegistrationNumberMaxLength).sample.get

        val otherEntityJourneyData =
          OtherEntityJourneyData.empty().copy(companyRegistrationNumber = Some(companyNumber))
        val updatedRegistration    = registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubGetRegistrationAdditionalInfo(additionalInfo)

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.CompanyRegistrationNumberController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", companyNumber))
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
