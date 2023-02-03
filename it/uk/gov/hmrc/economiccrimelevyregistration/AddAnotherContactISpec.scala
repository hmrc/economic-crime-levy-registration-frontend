package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class AddAnotherContactISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${contacts.routes.AddAnotherContactController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.AddAnotherContactController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the Add another contact HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(contacts.routes.AddAnotherContactController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"Would you like to add another contact?")
    }
  }

  s"POST ${contacts.routes.AddAnotherContactController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.AddAnotherContactController.onSubmit(NormalMode)
    )

    "save the selected answer then redirect the second contact name page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(contacts = registration.contacts.copy(secondContact = Some(true)))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.AddAnotherContactController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactNameController.onPageLoad(NormalMode).url)
    }

    "save the selected answer then redirect to the registered office address page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = random[Registration]
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        random[IncorporatedEntityJourneyDataWithValidCompanyProfile]

      val updatedRegistration = registration.copy(
        contacts = registration.contacts.copy(secondContact = Some(false), secondContactDetails = ContactDetails.empty),
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.AddAnotherContactController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ConfirmContactAddressController.onPageLoad(NormalMode).url)
    }
  }
}
