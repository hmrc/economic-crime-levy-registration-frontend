package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class FirstContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  val numberMaxLength: Int = 24

  s"GET ${contacts.routes.FirstContactNumberController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.FirstContactNumberController.onPageLoad())

    "respond with 200 status and the first contact number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]

      stubGetRegistration(
        registration.copy(contacts =
          registration.contacts.copy(firstContactDetails =
            registration.contacts.firstContactDetails.copy(name = Some(name))
          )
        )
      )

      val result = callRoute(FakeRequest(contacts.routes.FirstContactNumberController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's telephone number?")
    }
  }

  s"POST ${contacts.routes.FirstContactNumberController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.FirstContactNumberController.onSubmit())

    "save the provided telephone number then redirect to the add another contact page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]
      val number       = telephoneNumber(numberMaxLength).sample.get

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(firstContactDetails =
          registration.contacts.firstContactDetails.copy(name = Some(name), telephoneNumber = Some(number))
        )
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.FirstContactNumberController.onSubmit()).withFormUrlEncodedBody(("value", number))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.AddAnotherContactController.onPageLoad().url)
    }
  }
}
