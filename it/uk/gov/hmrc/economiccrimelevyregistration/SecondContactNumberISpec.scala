package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  val numberMaxLength: Int = 24

  s"GET ${contacts.routes.SecondContactNumberController.onPageLoad().url}" should {
    behave like authorisedActionRoute(contacts.routes.SecondContactNumberController.onPageLoad())

    "respond with 200 status and the second contact number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]

      stubGetRegistration(
        registration.copy(contacts =
          registration.contacts.copy(secondContactDetails =
            registration.contacts.secondContactDetails.copy(name = Some(name))
          )
        )
      )

      val result = callRoute(FakeRequest(contacts.routes.SecondContactNumberController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's telephone number?")
    }
  }

  s"POST ${contacts.routes.SecondContactNumberController.onSubmit().url}"  should {
    behave like authorisedActionRoute(contacts.routes.SecondContactNumberController.onSubmit())

    "save the provided telephone number then redirect to the add another contact page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                                         = random[Registration]
      val incorporatedEntityJourneyDataWithValidCompanyProfile =
        random[IncorporatedEntityJourneyDataWithValidCompanyProfile]
      val name                                                 = random[String]
      val number                                               = telephoneNumber(numberMaxLength).sample.get

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
        FakeRequest(contacts.routes.SecondContactNumberController.onSubmit()).withFormUrlEncodedBody(("value", number))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ConfirmContactAddressController.onPageLoad().url)
    }
  }
}
