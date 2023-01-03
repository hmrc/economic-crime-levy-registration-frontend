package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  val nameMaxLength: Int = 160

  s"GET ${contacts.routes.SecondContactNameController.onPageLoad().url}" should {
    behave like authorisedActionRoute(contacts.routes.SecondContactNameController.onPageLoad())

    "respond with 200 status and the second contact name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(contacts.routes.SecondContactNameController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Provide a second contact name")
    }
  }

  s"POST ${contacts.routes.SecondContactNameController.onSubmit().url}"  should {
    behave like authorisedActionRoute(contacts.routes.SecondContactNameController.onSubmit())

    "save the provided name then redirect to the second contact role page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = stringsWithMaxLength(nameMaxLength).sample.get

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(secondContactDetails =
          registration.contacts.secondContactDetails.copy(name = Some(name))
        )
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.SecondContactNameController.onSubmit()).withFormUrlEncodedBody(("value", name))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactRoleController.onPageLoad().url)
    }
  }
}
