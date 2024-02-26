package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.NameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${contacts.routes.SecondContactNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.SecondContactNameController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the second contact name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(contacts.routes.SecondContactNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"Provide a second contact name")
    }
  }

  s"POST ${contacts.routes.SecondContactNameController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.SecondContactNameController.onSubmit(NormalMode)
    )

    "save the provided name then redirect to the second contact role page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val name           = stringsWithMaxLength(NameMaxLength).sample.get
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(secondContactDetails =
          registration.contacts.secondContactDetails.copy(name = Some(name))
        )
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.SecondContactNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", name))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactRoleController.onPageLoad(NormalMode).url)
    }
  }
}
