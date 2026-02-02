package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.emailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class SecondContactEmailISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.SecondContactEmailController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactEmailController.onPageLoad(mode)
      )

      "respond with 200 status and the second contact email HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name           = arbitrary[String].sample.get
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

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

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name           = arbitrary[String].sample.get
        val email          = emailAddress(emailMaxLength).sample.get
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

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
                routes.CheckYourAnswersController.onPageLoad().url
              )
            }
        }

      }
    }
  }
}
