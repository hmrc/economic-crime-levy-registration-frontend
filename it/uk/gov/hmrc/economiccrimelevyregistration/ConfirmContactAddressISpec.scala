package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models._

class ConfirmContactAddressISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ConfirmContactAddressController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ConfirmContactAddressController.onPageLoad())

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

      stubGetRegistration(updatedRegistration)

      val result = callRoute(FakeRequest(routes.ConfirmContactAddressController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Do you want to use this registered address as the main contact address?")
    }
  }

  s"POST ${routes.ConfirmContactAddressController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.ConfirmContactAddressController.onSubmit())

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

      stubGetRegistration(updatedRegistration)

      stubUpsertRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration.copy(contactAddress = updatedRegistration.grsAddressToEclAddress))

      val result = callRoute(
        FakeRequest(routes.ConfirmContactAddressController.onSubmit()).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }

    "save the selected answer then redirect to is address in UK page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.ConfirmContactAddressController.onSubmit()).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.IsUkAddressController.onPageLoad().url)
    }
  }
}
