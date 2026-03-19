package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class ConfirmContactAddressISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ConfirmContactAddressController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.ConfirmContactAddressController.onPageLoad(NormalMode))

    "respond with 200 status and the confirm contact address HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile].sample.get

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )
      val additionalInfo      = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.ConfirmContactAddressController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"Do you want to use this registered address as the main contact address?")
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${routes.ConfirmContactAddressController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.ConfirmContactAddressController.onSubmit(mode))

      s"save the selected answer then redirect to check your answers page when the Yes option is selected in $mode" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration                                         = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val incorporatedEntityJourneyDataWithValidCompanyProfile =
          arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile].sample.get

        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = Some(true),
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )
        val additionalInfo      = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)

        stubUpsertRegistration(updatedRegistration)
        stubUpsertRegistration(updatedRegistration.copy(contactAddress = updatedRegistration.grsAddressToEclAddress))

        val result = callRoute(
          FakeRequest(routes.ConfirmContactAddressController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", "true"))
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(
          routes.CheckYourAnswersController.onPageLoad().url
        )
      }

      s"save the selected answer then redirect to is address in UK page when the No option is selected in $mode" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )

        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)

        val updatedRegistration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false), contactAddress = None)

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.ConfirmContactAddressController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", "false"))
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.IsUkAddressController.onPageLoad(mode).url)
      }
    }
  }
}
