package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.NameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class FirstContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${contacts.routes.FirstContactNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.FirstContactNameController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the first contact name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(contacts.routes.FirstContactNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"Provide a contact name")
    }
  }

  s"POST ${contacts.routes.FirstContactNameController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.FirstContactNameController.onSubmit(NormalMode))

    "save the provided name then redirect to the first contact role page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val name           = stringsWithMaxLength(NameMaxLength).sample.get
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(firstContactDetails =
          registration.contacts.firstContactDetails.copy(name = Some(name))
        )
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.FirstContactNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", name))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.FirstContactRoleController.onPageLoad(NormalMode).url)
    }
  }
}
