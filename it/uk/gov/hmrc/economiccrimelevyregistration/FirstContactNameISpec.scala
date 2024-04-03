package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.NameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class FirstContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.FirstContactNameController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.FirstContactNameController.onPageLoad(mode)
      )

      "respond with 200 status and the first contact name HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(contacts.routes.FirstContactNameController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include(s"Provide a contact name")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${contacts.routes.FirstContactNameController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.FirstContactNameController.onSubmit(mode))

      "save the provided name then redirect to the first contact role page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name           = stringsWithMaxLength(NameMaxLength).sample.get
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(firstContactDetails =
            registration.contacts.firstContactDetails.copy(name = Some(name))
          )
        )

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(contacts.routes.FirstContactNameController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", name))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              contacts.routes.FirstContactRoleController.onPageLoad(NormalMode).url
            )
          case CheckMode  => redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
        }

      }
    }
  }
}
