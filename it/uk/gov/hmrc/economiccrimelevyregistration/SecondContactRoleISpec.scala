package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.roleMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactRoleISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.SecondContactRoleController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactRoleController.onPageLoad(mode)
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
        stubGetRegistrationWithEmptyAdditionalInfo(
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(name = Some(name))
            )
          )
        )
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(contacts.routes.SecondContactRoleController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include(s"What is $name's role in your organisation?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${contacts.routes.SecondContactRoleController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactRoleController.onSubmit(mode)
      )

      "save the provided role then redirect to the correct page" in {
        stubAuthorisedWithNoGroupEnrolment()
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        val registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val name         = random[String]
        val role         = stringsWithMaxLength(roleMaxLength).sample.get

        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(secondContactDetails =
            registration.contacts.secondContactDetails.copy(name = Some(name), role = Some(role))
          )
        )

        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(contacts.routes.SecondContactRoleController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", role))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              contacts.routes.SecondContactEmailController.onPageLoad(NormalMode).url
            )
          case CheckMode  =>
            updatedRegistration.contacts.secondContactDetails match {
              case ContactDetails(Some(_), Some(_), Some(_), Some(_)) =>
                redirectLocation(result) shouldBe Some(
                  routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
                )
              case _                                                  =>
                redirectLocation(result) shouldBe Some(
                  contacts.routes.SecondContactEmailController.onPageLoad(CheckMode).url
                )
            }

        }
      }
    }
  }
}
