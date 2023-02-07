package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{RegistrationSubmittedEmailParameters, RegistrationSubmittedEmailRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, Languages, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils

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
      val eclReference      = random[String]
      val contactDetails    = random[ContactDetails]
      val firstContactName  = random[String]
      val firstContactEmail = random[String]

      val registrationWithOneContact = registration.copy(contacts =
        Contacts(
          firstContactDetails =
            contactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContact = Some(false),
          secondContactDetails = ContactDetails.empty
        )
      )

      stubGetRegistration(registrationWithOneContact)

      stubSubmitRegistration(eclReference)

      stubSendRegistrationSubmittedEmail(
        RegistrationSubmittedEmailRequest(
          to = Seq(firstContactEmail),
          parameters = RegistrationSubmittedEmailParameters(
            name = firstContactName,
            eclRegistrationReference = eclReference,
            dateDue = ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)(messagesApi.preferred(Seq(Languages.english)))
          )
        )
      )

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onSubmit()))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.RegistrationSubmittedController.onPageLoad().url)
    }
  }

}
