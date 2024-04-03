package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactEmailISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.SecondContactEmailController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactEmailController.onPageLoad(mode)
      )

      "respond with 200 status and the second contact email HTML view" in {
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
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(name = Some(name))
            )
          )
        )
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(contacts.routes.SecondContactEmailController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include(s"What is $name's email address?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${contacts.routes.SecondContactEmailController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactEmailController.onSubmit(mode)
      )

      "save the provided email address then redirect to the second contact telephone number page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name           = random[String]
        val email          = emailAddress(EmailMaxLength).sample.get
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(secondContactDetails =
            registration.contacts.secondContactDetails.copy(name = Some(name), emailAddress = Some(email.toLowerCase))
          )
        )

        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(contacts.routes.SecondContactEmailController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", email))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              contacts.routes.SecondContactNumberController.onPageLoad(NormalMode).url
            )
          case CheckMode  =>
            if (updatedRegistration.contacts.secondContactDetails.telephoneNumber.isEmpty) {
              redirectLocation(result) shouldBe Some(contacts.routes.SecondContactNumberController.onPageLoad(mode).url)
            } else {
              redirectLocation(result) shouldBe Some(
                routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
              )
            }
        }

      }
    }
  }
}
