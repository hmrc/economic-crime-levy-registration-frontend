package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.telephoneNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class SecondContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${contacts.routes.SecondContactNumberController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactNumberController.onPageLoad(mode)
      )

      "respond with 200 status and the second contact number HTML view" in {
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

        val result = callRoute(FakeRequest(contacts.routes.SecondContactNumberController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include(s"What is $name's telephone number?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${contacts.routes.SecondContactNumberController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        contacts.routes.SecondContactNumberController.onSubmit(mode)
      )

      "save the provided telephone number then redirect to the correct page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration                                         = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val incorporatedEntityJourneyDataWithValidCompanyProfile =
          arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile].sample.get
        val name                                                 = arbitrary[String].sample.get
        val number                                               = telephoneNumber(telephoneNumberMaxLength).sample.get
        val additionalInfo                                       = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        val updatedRegistration = registration.copy(
          contacts = registration.contacts.copy(secondContactDetails =
            registration.contacts.secondContactDetails.copy(name = Some(name), telephoneNumber = Some(number))
          ),
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(contacts.routes.SecondContactNumberController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", number))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            updatedRegistration.grsAddressToEclAddress match {
              case Some(_) =>
                redirectLocation(result) shouldBe Some(
                  routes.ConfirmContactAddressController.onPageLoad(NormalMode).url
                )
              case _       => redirectLocation(result) shouldBe Some(routes.IsUkAddressController.onPageLoad(NormalMode).url)
            }
          case CheckMode  =>
            updatedRegistration.contacts.secondContactDetails match {
              case ContactDetails(Some(_), Some(_), Some(_), Some(_)) =>
                redirectLocation(result) shouldBe Some(
                  routes.CheckYourAnswersController.onPageLoad().url
                )
              case _                                                  =>
                redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
            }
        }
      }
    }
  }
}
