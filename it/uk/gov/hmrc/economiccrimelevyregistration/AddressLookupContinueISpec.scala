package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
class AddressLookupContinueISpec extends ISpecBase with AuthorisedBehaviour {

  val journeyId: String = "test-journey-id"

  s"GET ${routes.AddressLookupContinueController.continue(journeyId).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AddressLookupContinueController.continue(journeyId))

    "retrieve the ALF address data, update the registration with the address and continue the registration journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

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
            countryCode = alfAddressData.address.country.code
          )
        )
      )

      stubGetAlfAddressData(journeyId, alfAddressData)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(FakeRequest(routes.AddressLookupContinueController.continue(journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }
  }

}
