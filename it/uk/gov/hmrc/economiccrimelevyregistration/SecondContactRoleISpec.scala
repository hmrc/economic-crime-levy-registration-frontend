package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.RoleMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactRoleISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${contacts.routes.SecondContactRoleController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.SecondContactRoleController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the second contact role HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
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
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(contacts.routes.SecondContactRoleController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's role?")
    }
  }

  s"POST ${contacts.routes.SecondContactRoleController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.SecondContactRoleController.onSubmit(NormalMode)
    )

    "save the provided role then redirect to the second contact email page" in {
      stubAuthorisedWithNoGroupEnrolment()
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val name         = random[String]
      val role         = stringsWithMaxLength(RoleMaxLength).sample.get

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(secondContactDetails =
          registration.contacts.secondContactDetails.copy(name = Some(name), role = Some(role))
        )
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.SecondContactRoleController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", role))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactEmailController.onPageLoad(NormalMode).url)
    }
  }
}
