package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.emailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class FirstContactEmailISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.FirstContactEmailController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.FirstContactEmailController.onPageLoad(mode)
      )

      "respond with 200 status and the first contact email HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name           = random[String]
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(
          registration.copy(contacts =
            registration.contacts.copy(firstContactDetails =
              registration.contacts.firstContactDetails.copy(name = Some(name))
            )
          )
        )

        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(contacts.routes.FirstContactEmailController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include(s"What is $name's email address?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${contacts.routes.FirstContactEmailController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.FirstContactEmailController.onSubmit(mode)
      )

      "save the provided email address then redirect to the correct page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name         = random[String]
        val email        = emailAddress(emailMaxLength).sample.get

        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(firstContactDetails =
            registration.contacts.firstContactDetails.copy(name = Some(name), emailAddress = Some(email.toLowerCase))
          )
        )
        val additionalInfo      = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(contacts.routes.FirstContactEmailController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", email))
        )

        status(result) shouldBe SEE_OTHER

        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              contacts.routes.FirstContactNumberController.onPageLoad(NormalMode).url
            )
          case CheckMode  =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad().url
            )
        }
      }
    }
  }
}
