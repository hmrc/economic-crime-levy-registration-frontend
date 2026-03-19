package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class IsUkAddressISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.IsUkAddressController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.IsUkAddressController.onPageLoad(mode))

      "respond with 200 status and the is UK address HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.IsUkAddressController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Is your contact address based in the UK?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${routes.IsUkAddressController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.IsUkAddressController.onSubmit(mode))

      "save the selected address option then redirect to the address lookup frontend journey" in {
        stubAuthorisedWithNoGroupEnrolment()

        val contactAddressIsUk = arbitrary[Boolean].sample.get
        val registration       = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo     = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)

        val alfLabels = AlfEnCyLabels(appConfig)

        val expectedJourneyConfig: AlfJourneyConfig =
          AlfJourneyConfig(
            options = AlfOptions(
              continueUrl =
                s"http://localhost:14000/register-for-economic-crime-levy/address-lookup-continue/${mode.toString.toLowerCase}",
              homeNavHref = "/register-for-economic-crime-levy",
              signOutHref = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
              accessibilityFooterUrl = "/accessibility-statement/economic-crime-levy",
              deskProServiceName = "economic-crime-levy-registration-frontend",
              ukMode = contactAddressIsUk
            ),
            labels = alfLabels
          )

        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubInitAlfJourney(expectedJourneyConfig)

        val updatedRegistration = registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.IsUkAddressController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", contactAddressIsUk.toString))
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some("test-url")
      }
    }
  }
}
