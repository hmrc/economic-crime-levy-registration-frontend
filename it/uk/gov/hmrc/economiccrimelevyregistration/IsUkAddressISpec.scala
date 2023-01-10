package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._

class IsUkAddressISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.IsUkAddressController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.IsUkAddressController.onPageLoad())

    "respond with 200 status and the is UK address HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.IsUkAddressController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Is your contact address based in the UK?")
    }
  }

  s"POST ${routes.IsUkAddressController.onSubmit().url}" should {
    behave like authorisedActionRoute(routes.IsUkAddressController.onSubmit())

    "save the selected address option then redirect to the address lookup frontend journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val contactAddressIsUk = random[Boolean]
      val registration = random[Registration]
      val appConfig = app.injector.instanceOf[AppConfig]
      implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

      val alfLabels = AlfEnCyLabels(appConfig)

      val expectedJourneyConfig: AlfJourneyConfig =
        AlfJourneyConfig(
          options = AlfOptions(
            continueUrl = "http://localhost:14000/register-for-the-economic-crime-levy/address-lookup-continue",
            homeNavHref = "/register-for-the-economic-crime-levy",
            signOutHref = "http://localhost:14000/register-for-the-economic-crime-levy/account/sign-out-survey",
            accessibilityFooterUrl = "/accessibility-statement/register-for-the-economic-crime-levy",
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
        FakeRequest(routes.IsUkAddressController.onSubmit()).withFormUrlEncodedBody(("value", contactAddressIsUk.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }
  }
}
