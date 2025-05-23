package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclAddress, EntityType, NormalMode, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
class AddressLookupContinueISpec extends ISpecBase with AuthorisedBehaviour {

  val journeyId: String = "test-journey-id"

  s"GET ${routes.AddressLookupContinueController.continue(NormalMode, journeyId).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.AddressLookupContinueController.continue(NormalMode, journeyId)
    )

    "retrieve the ALF address data, update the registration with the address and continue the registration journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val alfAddressData = random[AlfAddressData]

      val updatedRegistration = registration.copy(contactAddress =
        Some(
          EclAddress(
            organisation = alfAddressData.address.organisation,
            addressLine1 = alfAddressData.address.lines.headOption,
            addressLine2 = alfAddressData.address.lines.lift(1),
            addressLine3 = alfAddressData.address.lines.lift(2),
            addressLine4 = alfAddressData.address.lines.lift(3),
            region = None,
            postCode = alfAddressData.address.postcode,
            poBox = alfAddressData.address.poBox,
            countryCode = Some(alfAddressData.address.country.code)
          )
        )
      )

      stubGetAlfAddressData(journeyId, alfAddressData)

      stubUpsertRegistration(updatedRegistration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.AddressLookupContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad().url
      )
    }
  }

  s"GET ${routes.AddressLookupContinueController.continue(CheckMode, journeyId).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.AddressLookupContinueController.continue(CheckMode, journeyId)
    )

    "retrieve the ALF address data, update the registration with the address and go to check your answers page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val alfAddressData = random[AlfAddressData]

      val updatedRegistration = registration.copy(contactAddress =
        Some(
          EclAddress(
            organisation = alfAddressData.address.organisation,
            addressLine1 = alfAddressData.address.lines.headOption,
            addressLine2 = alfAddressData.address.lines.lift(1),
            addressLine3 = alfAddressData.address.lines.lift(2),
            addressLine4 = alfAddressData.address.lines.lift(3),
            region = None,
            postCode = alfAddressData.address.postcode,
            poBox = alfAddressData.address.poBox,
            countryCode = Some(alfAddressData.address.country.code)
          )
        )
      )

      stubGetAlfAddressData(journeyId, alfAddressData)

      stubUpsertRegistration(updatedRegistration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.AddressLookupContinueController.continue(CheckMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad().url
      )
    }
  }

}
