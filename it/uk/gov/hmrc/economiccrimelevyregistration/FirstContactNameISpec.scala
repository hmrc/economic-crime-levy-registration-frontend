package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.models._

class FirstContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  val nameMaxLength: Int = 160

  s"GET ${contacts.routes.FirstContactNameController.onPageLoad().url}" should {
    behave like authorisedActionRoute(contacts.routes.FirstContactNameController.onPageLoad())

    "respond with 200 status and the first contact name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(contacts.routes.FirstContactNameController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Provide a contact name")
    }
  }

  s"POST ${contacts.routes.FirstContactNameController.onSubmit().url}"  should {
    behave like authorisedActionRoute(contacts.routes.FirstContactNameController.onSubmit())

    "save the provided name then redirect to the first contact role page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = stringsWithMaxLength(nameMaxLength).sample.get

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(firstContactDetails =
          registration.contacts.firstContactDetails.copy(name = Some(name))
        )
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.FirstContactNameController.onSubmit()).withFormUrlEncodedBody(("value", name))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.FirstContactRoleController.onPageLoad().url)
    }
  }
}
