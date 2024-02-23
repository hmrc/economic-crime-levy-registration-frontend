package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class ConfirmContactAddressISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ConfirmContactAddressController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.ConfirmContactAddressController.onPageLoad(NormalMode))

    "respond with 200 status and the confirm contact address HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = random[Registration]
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        random[IncorporatedEntityJourneyDataWithValidCompanyProfile]

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )
      val additionalInfo      = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(updatedRegistration)
      stubSessionForStoreUrl(routes.ConfirmContactAddressController.onPageLoad(NormalMode))

      val result = callRoute(FakeRequest(routes.ConfirmContactAddressController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"Do you want to use this registered address as the main contact address?")
    }
  }

  s"POST ${routes.ConfirmContactAddressController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.ConfirmContactAddressController.onSubmit(NormalMode))

    "save the selected answer then redirect to check your answers page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = random[Registration]
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        random[IncorporatedEntityJourneyDataWithValidCompanyProfile]

      val updatedRegistration = registration.copy(
        useRegisteredOfficeAddressAsContactAddress = Some(true),
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )
      val additionalInfo      = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(updatedRegistration)

      stubUpsertRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration.copy(contactAddress = updatedRegistration.grsAddressToEclAddress))

      val result = callRoute(
        FakeRequest(routes.ConfirmContactAddressController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }

    "save the selected answer then redirect to is address in UK page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)

      val updatedRegistration =
        registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false), contactAddress = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.ConfirmContactAddressController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.IsUkAddressController.onPageLoad(NormalMode).url)
    }
  }
}
