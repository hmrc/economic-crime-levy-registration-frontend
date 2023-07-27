package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.Trust
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{RegistrationSubmittedEmailParameters, RegistrationSubmittedEmailRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, EntityType, Languages, OtherEntityJourneyData, OtherEntityType, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils

import java.nio.file.{Files, Paths}
import java.time.{LocalDate, ZoneOffset}

class CheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CheckYourAnswersController.onPageLoad())

    "respond with 200 status and the Check your answers HTML view when the registration data is valid" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val errors       = random[DataValidationErrors]

      stubGetRegistration(registration)
      stubGetRegistrationValidationErrors(valid = true, errors)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Check your answers")
    }

    "redirect to the journey recovery page when the registration data is invalid" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val errors       = random[DataValidationErrors]

      stubGetRegistration(registration)
      stubGetRegistrationValidationErrors(valid = false, errors)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onPageLoad()))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }
  }

  s"POST ${routes.CheckYourAnswersController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CheckYourAnswersController.onSubmit())

    "redirect to the registration submitted page after submitting the registration successfully" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration      = random[Registration]
      val entityType        = random[EntityType]
      val eclReference      = random[String]
      val contactDetails    = random[ContactDetails]
      val firstContactName  = random[String]
      val firstContactEmail = random[String]
      val otherEntityType = random[OtherEntityType]

      val registrationWithOneContact = registration.copy(
        optOtherEntityJourneyData = Some(OtherEntityJourneyData.empty().copy(
          entityType = Some(otherEntityType)
        )),
        contacts = Contacts(
          firstContactDetails =
            contactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContact = Some(false),
          secondContactDetails = ContactDetails.empty
        ),
        entityType = Some(entityType)
      )

      stubGetRegistration(registrationWithOneContact)

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
            dateDue = ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)(
              messagesApi.preferred(Seq(Languages.english))
            ),
            "true",
            None
          )
        )
      )

      val call = entityType match {
        case Other => routes.RegistrationReceivedController.onPageLoad().url
        case _     => routes.RegistrationSubmittedController.onPageLoad().url
      }

      stubDeleteRegistration()

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onSubmit()))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(call)

      eventually {
        verify(1, postRequestedFor(urlEqualTo("/hmrc/email")))
      }
    }
  }

}
