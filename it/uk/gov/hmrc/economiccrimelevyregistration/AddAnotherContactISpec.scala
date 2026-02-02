package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class AddAnotherContactISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.AddAnotherContactController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.AddAnotherContactController.onPageLoad(mode)
      )

      "respond with 200 status and the Add another contact HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        val registration = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )

        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(contacts.routes.AddAnotherContactController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include(s"Would you like to add another contact?")
      }
    }
  }

  s"POST ${contacts.routes.AddAnotherContactController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.AddAnotherContactController.onSubmit(NormalMode)
    )

    "save the selected answer then redirect the second contact name page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val updatedRegistration = registration.copy(contacts = registration.contacts.copy(secondContact = Some(true)))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.AddAnotherContactController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactNameController.onPageLoad(NormalMode).url)
    }

    "save the selected answer then redirect to the registered office address page when the No option is selected and there is no existing second contact" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile].sample.get

      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val updatedRegistration = registration.copy(
        contacts = registration.contacts.copy(secondContact = Some(false), secondContactDetails = ContactDetails.empty),
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )

      stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.AddAnotherContactController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ConfirmContactAddressController.onPageLoad(NormalMode).url)
    }

    "save the selected answer then redirect to the registered office address page when the No option is selected and there is an existing second contact" in {
      stubAuthorisedWithNoGroupEnrolment()
      val contactName                                          = arbitrary[String].sample.get
      val contactRole                                          = arbitrary[String].sample.get
      val contactEmail                                         = arbitrary[String].sample.get
      val contactTelephone                                     = arbitrary[String].sample.get
      val registration                                         = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile].sample.get

      val regWithSecondContact = registration
        .copy(contacts =
          registration.contacts.copy(
            secondContact = Some(true),
            secondContactDetails =
              ContactDetails(Some(contactName), Some(contactRole), Some(contactEmail), Some(contactTelephone))
          )
        )
        .copy(
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)

      val updatedRegistration = regWithSecondContact.copy(
        contacts =
          regWithSecondContact.contacts.copy(secondContact = Some(false), secondContactDetails = ContactDetails.empty),
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )

      stubGetRegistrationWithEmptyAdditionalInfo(regWithSecondContact)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.AddAnotherContactController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ConfirmContactAddressController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${contacts.routes.AddAnotherContactController.onSubmit(CheckMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.AddAnotherContactController.onSubmit(CheckMode)
    )

    "save the selected answer then redirect to the Check Your Answers page when the No option is selected and there is an existing second contact" in {
      stubAuthorisedWithNoGroupEnrolment()
      val contactName                                          = arbitrary[String].sample.get
      val contactRole                                          = arbitrary[String].sample.get
      val contactEmail                                         = arbitrary[String].sample.get
      val contactTelephone                                     = arbitrary[String].sample.get
      val registration                                         = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile].sample.get

      val regWithSecondContact = registration
        .copy(contacts =
          registration.contacts.copy(
            secondContact = Some(true),
            secondContactDetails =
              ContactDetails(Some(contactName), Some(contactRole), Some(contactEmail), Some(contactTelephone))
          )
        )
        .copy(
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)

      val updatedRegistration = regWithSecondContact.copy(
        contacts =
          regWithSecondContact.contacts.copy(secondContact = Some(false), secondContactDetails = ContactDetails.empty),
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )

      stubGetRegistrationWithEmptyAdditionalInfo(regWithSecondContact)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.AddAnotherContactController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad().url
      )
    }

  }
}
