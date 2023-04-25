/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.FakeValidatedRegistrationAction
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, CreateEclSubscriptionResponse, Registration, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.EmailService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val view: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val mockEmailService: EmailService                         = mock[EmailService]

  class TestContext(registrationData: Registration) {
    val controller = new CheckYourAnswersController(
      messagesApi,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      mcc,
      view,
      new FakeValidatedRegistrationAction(registrationData),
      mockEmailService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { registration: Registration =>
      new TestContext(registration) {
        implicit val registrationDataRequest: RegistrationDataRequest[AnyContentAsEmpty.type] =
          RegistrationDataRequest(fakeRequest, registration.internalId, registration)
        implicit val messages: Messages                                                       = messagesApi.preferred(registrationDataRequest)

        val result: Future[Result] = controller.onPageLoad()(registrationDataRequest)

        val organisationDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            EntityTypeSummary.row(),
            EntityNameSummary.row(),
            CompanyNumberSummary.row(),
            CtUtrSummary.row(),
            SaUtrSummary.row(),
            NinoSummary.row(),
            DateOfBirthSummary.row(),
            AmlRegulatedActivitySummary.row(),
            RelevantAp12MonthsSummary.row(),
            RelevantApLengthSummary.row(),
            UkRevenueSummary.row(),
            AmlSupervisorSummary.row(),
            BusinessSectorSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        val contactDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            FirstContactNameSummary.row(),
            FirstContactRoleSummary.row(),
            FirstContactEmailSummary.row(),
            FirstContactNumberSummary.row(),
            SecondContactSummary.row(),
            SecondContactNameSummary.row(),
            SecondContactRoleSummary.row(),
            SecondContactEmailSummary.row(),
            SecondContactNumberSummary.row(),
            UseRegisteredAddressSummary.row(),
            ContactAddressSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(organisationDetails, contactDetails)(
          fakeRequest,
          messages
        ).toString
      }
    }
  }

  "onSubmit" should {
    "redirect to the registration submitted page after submitting the registration and sending email successfully" in forAll(
      Arbitrary.arbitrary[CreateEclSubscriptionResponse],
      Arbitrary.arbitrary[Registration],
      emailAddress(EmailMaxLength),
      Arbitrary.arbitrary[Boolean],
      emailAddress(EmailMaxLength)
    ) {
      (
        createEclSubscriptionResponse: CreateEclSubscriptionResponse,
        registration: Registration,
        firstContactEmailAddress: String,
        secondContact: Boolean,
        secondContactEmailAddress: String
      ) =>
        val updatedRegistration = registration.copy(
          contacts = Contacts(
            firstContactDetails = validContactDetails.copy(emailAddress = Some(firstContactEmailAddress)),
            secondContact = Some(secondContact),
            secondContactDetails = if (secondContact) {
              validContactDetails.copy(emailAddress = Some(secondContactEmailAddress))
            } else { ContactDetails.empty }
          )
        )

        new TestContext(updatedRegistration) {
          when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
            .thenReturn(Future.successful(updatedRegistration))

          when(
            mockEclRegistrationConnector.submitRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any())
          )
            .thenReturn(Future.successful(createEclSubscriptionResponse))

          when(
            mockEclRegistrationConnector.deleteRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any())
          )
            .thenReturn(Future.successful(()))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result)                                             shouldBe SEE_OTHER
          session(result).get(SessionKeys.EclReference)              shouldBe Some(createEclSubscriptionResponse.eclReference)
          session(result).get(SessionKeys.FirstContactEmailAddress)  shouldBe Some(firstContactEmailAddress)
          session(result).get(SessionKeys.SecondContact)             shouldBe Some(secondContact.toString)
          session(result).get(SessionKeys.SecondContactEmailAddress) shouldBe {
            if (secondContact) Some(secondContactEmailAddress) else Some("")
          }
          redirectLocation(result)                                   shouldBe Some(routes.RegistrationSubmittedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendRegistrationSubmittedEmails(
            ArgumentMatchers.eq(updatedRegistration.contacts),
            ArgumentMatchers.eq(createEclSubscriptionResponse.eclReference)
          )(any(), any())

          reset(mockEmailService)
        }
    }

    "throw an IllegalStateException when the first contact email address is not present in the registration" in forAll {
      (createEclSubscriptionResponse: CreateEclSubscriptionResponse, registration: Registration) =>
        val updatedRegistration = registration.copy(
          contacts = Contacts(
            firstContactDetails = validContactDetails.copy(emailAddress = None),
            secondContact = Some(false),
            secondContactDetails = ContactDetails.empty
          )
        )

        new TestContext(updatedRegistration) {
          when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
            .thenReturn(Future.successful(updatedRegistration))

          when(
            mockEclRegistrationConnector.submitRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any())
          )
            .thenReturn(Future.successful(createEclSubscriptionResponse))

          when(
            mockEclRegistrationConnector.deleteRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any())
          )
            .thenReturn(Future.successful(()))

          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onSubmit()(fakeRequest))
          }

          result.getMessage shouldBe "First contact email address not found in registration data"

          verify(mockEmailService, times(1)).sendRegistrationSubmittedEmails(
            ArgumentMatchers.eq(updatedRegistration.contacts),
            ArgumentMatchers.eq(createEclSubscriptionResponse.eclReference)
          )(any(), any())

          reset(mockEmailService)
        }
    }

    "throw an IllegalStateException when the second contact is not present in the registration" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          contacts = Contacts(
            firstContactDetails = validContactDetails,
            secondContact = None,
            secondContactDetails = ContactDetails.empty
          )
        )

        new TestContext(updatedRegistration) {
          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onSubmit()(fakeRequest))
          }

          result.getMessage shouldBe "Second contact not found in registration data"
        }
    }

    "throw an IllegalStateException when the second contact email address is not present in the registration" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          contacts = Contacts(
            firstContactDetails = validContactDetails,
            secondContact = Some(true),
            secondContactDetails = validContactDetails.copy(emailAddress = None)
          )
        )

        new TestContext(updatedRegistration) {
          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onSubmit()(fakeRequest))
          }

          result.getMessage shouldBe "Second contact email address not found in registration data"
        }
    }
  }
}
