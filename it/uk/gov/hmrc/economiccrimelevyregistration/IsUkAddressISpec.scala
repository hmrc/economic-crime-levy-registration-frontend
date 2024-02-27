package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._

class IsUkAddressISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.IsUkAddressController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.IsUkAddressController.onPageLoad(NormalMode))

    "respond with 200 status and the is UK address HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.IsUkAddressController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Is your contact address based in the UK?")
    }
  }

  s"POST ${routes.IsUkAddressController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.IsUkAddressController.onSubmit(NormalMode))

    "save the selected address option then redirect to the address lookup frontend journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val contactAddressIsUk = random[Boolean]
      val registration       = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo     = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)

      val alfLabels = AlfEnCyLabels(appConfig)

      val expectedJourneyConfig: AlfJourneyConfig =
        AlfJourneyConfig(
          options = AlfOptions(
            continueUrl = "http://localhost:14000/register-for-economic-crime-levy/address-lookup-continue/normalmode",
            homeNavHref = "/register-for-economic-crime-levy",
            signOutHref = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
            accessibilityFooterUrl = "/accessibility-statement/economic-crime-levy",
            deskProServiceName = "economic-crime-levy-registration-frontend",
            ukMode = contactAddressIsUk
          ),
          labels = alfLabels
        )

      stubGetRegistration(registration)
      stubInitAlfJourney(expectedJourneyConfig)

      val updatedRegistration = registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.IsUkAddressController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", contactAddressIsUk.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }
  }
}
