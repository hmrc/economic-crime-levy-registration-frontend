package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.CompanyRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.NonUKEstablishment
import uk.gov.hmrc.economiccrimelevyregistration.models._

class NonUkCrnISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.NonUkCrnController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.NonUkCrnController.onPageLoad(NormalMode))

    "respond with 200 status and the company registration number HTML view" in {
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

      val result = callRoute(FakeRequest(routes.NonUkCrnController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your company registration number?")
    }
  }

  s"POST ${routes.NonUkCrnController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.NonUkCrnController.onSubmit(NormalMode))

    "save the company registration number then redirect to the UTR type page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      val validRegistration = registration.copy(entityType = Some(NonUKEstablishment))
      stubGetRegistrationAdditionalInfo(additionalInfo)

      val companyNumber = stringsWithMaxLength(CompanyRegistrationNumberMaxLength).sample.get

      stubGetRegistrationWithEmptyAdditionalInfo(validRegistration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(companyRegistrationNumber = Some(companyNumber))
      val updatedRegistration    = validRegistration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.NonUkCrnController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", companyNumber))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UtrTypeController.onPageLoad(NormalMode).url)
    }
  }

}
