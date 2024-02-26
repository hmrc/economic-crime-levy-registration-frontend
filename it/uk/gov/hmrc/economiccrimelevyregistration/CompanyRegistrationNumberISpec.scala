package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.CompanyRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CompanyRegistrationNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CompanyRegistrationNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the company registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val companyNumber: String = stringsWithMaxLength(CompanyRegistrationNumberMaxLength).sample.get

      val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(companyRegistrationNumber = Some(companyNumber))

      val registration: Registration = random[Registration]
        .copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

      val additionalInfo: RegistrationAdditionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistration(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your company registration number?")
    }
  }

  s"POST ${routes.CompanyRegistrationNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CompanyRegistrationNumberController.onSubmit(NormalMode))

    "save the company registration number then redirect to the check your answers page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      val companyNumber = stringsWithMaxLength(CompanyRegistrationNumberMaxLength).sample.get

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(companyRegistrationNumber = Some(companyNumber))
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubGetRegistration(updatedRegistration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CompanyRegistrationNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", companyNumber))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
    }
  }

}
