package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.TelephoneNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${contacts.routes.SecondContactNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.SecondContactNumberController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the second contact number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val name           = random[String]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(
        registration.copy(contacts =
          registration.contacts.copy(secondContactDetails =
            registration.contacts.secondContactDetails.copy(name = Some(name))
          )
        )
      )
      stubSessionForStoreUrl(
        contacts.routes.SecondContactNumberController.onPageLoad(NormalMode),
        registration.internalId
      )

      val result = callRoute(FakeRequest(contacts.routes.SecondContactNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's telephone number?")
    }
  }

  s"POST ${contacts.routes.SecondContactNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.SecondContactNumberController.onSubmit(NormalMode)
    )

    "save the provided telephone number then redirect to the add another contact page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = random[Registration]
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        random[IncorporatedEntityJourneyDataWithValidCompanyProfile]
      val name                                                 = random[String]
      val number                                               = telephoneNumber(TelephoneNumberMaxLength).sample.get
      val additionalInfo                                       = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val updatedRegistration = registration.copy(
        contacts = registration.contacts.copy(secondContactDetails =
          registration.contacts.secondContactDetails.copy(name = Some(name), telephoneNumber = Some(number))
        ),
        incorporatedEntityJourneyData =
          Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.SecondContactNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", number))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ConfirmContactAddressController.onPageLoad(NormalMode).url)
    }
  }
}
