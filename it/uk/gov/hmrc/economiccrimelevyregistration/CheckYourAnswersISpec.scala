package uk.gov.hmrc.economiccrimelevyregistration

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{RegistrationSubmittedEmailParameters, RegistrationSubmittedEmailRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import java.time.{LocalDate, ZoneOffset}

class CheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CheckYourAnswersController.onPageLoad())

    "respond with 200 status and the Check your answers HTML view when the registration data is valid" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          registrationType = Some(Amendment),
          relevantApRevenue = Some(randomApRevenue())
        )
      val errors         = arbitrary[DataValidationError].sample.get
      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationValidationErrors(valid = true, errors)
      stubSessionForStoreUrl()

      val result = callRoute(
        FakeRequest(routes.CheckYourAnswersController.onPageLoad())
      )

      status(result) shouldBe OK

      html(result) should include(s"Check your answers")
    }

    "redirect to the journey recovery page when the registration data is invalid" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val errors         = arbitrary[DataValidationError].sample.get
      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationValidationErrors(valid = false, errors)
      stubSessionForStoreUrl()

      val result = callRoute(
        FakeRequest(routes.CheckYourAnswersController.onPageLoad())
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }
  }

  s"POST ${routes.CheckYourAnswersController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CheckYourAnswersController.onSubmit())

    "redirect to the registration submitted page after submitting the registration successfully" in {
      Arbitrary.arbitrary[EntityType].retryUntil(!EntityType.isOther(_)).map { entityType =>
        testOnSubmit(entityType, routes.RegistrationSubmittedController.onPageLoad().url)
      }
      Arbitrary.arbitrary[EntityType].retryUntil(EntityType.isOther).map { entityType =>
        testOnSubmit(entityType, routes.RegistrationReceivedController.onPageLoad().url)
      }
    }
  }

  private def testOnSubmit(entityType: EntityType, expectedRedirectUrl: String): Unit = {
    stubAuthorisedWithNoGroupEnrolment()

    val registration      = arbitrary[Registration].sample.get
      .copy(
        entityType = Some(arbitrary[EntityType].sample.get),
        relevantApRevenue = Some(randomApRevenue())
      )
    val eclReference      = arbitrary[String].sample.get
    val contactDetails    = arbitrary[ContactDetails].sample.get
    val firstContactName  = arbitrary[String].sample.get
    val firstContactEmail = arbitrary[String].sample.get

    val registrationWithOneContact = registration.copy(
      contacts = Contacts(
        firstContactDetails =
          contactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
        secondContact = Some(false),
        secondContactDetails = ContactDetails.empty
      ),
      entityType = Some(entityType),
      registrationType = Some(Initial)
    )

    val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

    stubGetRegistrationAdditionalInfo(additionalInfo)

    stubGetRegistrationWithEmptyAdditionalInfo(registrationWithOneContact)

    stubUpsertRegistrationWithoutRequestMatching(registrationWithOneContact)

    stubSubmitRegistration(eclReference)

    stubSendRegistrationSubmittedEmail(
      RegistrationSubmittedEmailRequest(
        to = Seq(firstContactEmail),
        parameters = RegistrationSubmittedEmailParameters(
          name = firstContactName,
          eclRegistrationReference = eclReference,
          eclRegistrationDate = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(
            messagesApi.preferred(Seq(Languages.english))
          ),
          dateDue = ViewUtils.formatLocalDate(testEclTaxYear.dateDue, translate = false)(
            messagesApi.preferred(Seq(Languages.english))
          ),
          "true",
          None,
          None,
          None
        )
      )
    )

    stubDeleteRegistration()

    val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onSubmit()))

    status(result)           shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirectUrl)

    eventually {
      verify(1, postRequestedFor(urlEqualTo("/hmrc/email")))
    }
  }
}
