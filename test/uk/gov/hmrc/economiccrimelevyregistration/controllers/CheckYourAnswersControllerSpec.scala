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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalacheck.Arbitrary
import org.scalacheck.Gen.choose
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.ValidRegistrationWithRegistrationType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyregistration.views.html._
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.util.Base64
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val view: CheckYourAnswersView             = app.injector.instanceOf[CheckYourAnswersView]
  val pdfView: OtherRegistrationPdfView      = app.injector.instanceOf[OtherRegistrationPdfView]
  val amendPdfView: AmendRegistrationPdfView = app.injector.instanceOf[AmendRegistrationPdfView]

  val mockEclRegistrationService: EclRegistrationService                       = mock[EclRegistrationService]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockEmailService: EmailService                                           = mock[EmailService]

  class TestContext(registrationData: Registration, additionalInfo: Option[RegistrationAdditionalInfo] = None) {
    val controller = new CheckYourAnswersController(
      messagesApi,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData, additionalInfo),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      mcc,
      mockRegistrationAdditionalInfoService,
      view,
      mockEmailService,
      pdfView,
      amendPdfView,
      appConfig
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { validRegistration: ValidRegistrationWithRegistrationType =>
      new TestContext(validRegistration.registration) {
        implicit val registrationDataRequest: RegistrationDataRequest[AnyContentAsEmpty.type] =
          RegistrationDataRequest(
            fakeRequest,
            validRegistration.registration.internalId,
            validRegistration.registration,
            None,
            Some(testEclRegistrationReference)
          )
        implicit val messages: Messages                                                       = messagesApi.preferred(registrationDataRequest)

        when(mockEclRegistrationService.getRegistrationValidationErrors(anyString())(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Option[DataValidationError]](Future.successful(Right(None))))

        val result: Future[Result] = controller.onPageLoad()(registrationDataRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(
          CheckYourAnswersViewModel(
            validRegistration.registration,
            None,
            Some(testEclRegistrationReference),
            None
          )
        )(
          fakeRequest,
          messages
        ).toString
      }
    }

    "return a DataRetrieval error when getRegistrationValidationErrors returns an error" in forAll {
      validRegistration: ValidRegistrationWithRegistrationType =>
        new TestContext(validRegistration.registration) {
          implicit val registrationDataRequest: RegistrationDataRequest[AnyContentAsEmpty.type] =
            RegistrationDataRequest(
              fakeRequest,
              validRegistration.registration.internalId,
              validRegistration.registration,
              None,
              Some(testEclRegistrationReference)
            )
          implicit val messages: Messages                                                       = messagesApi.preferred(registrationDataRequest)

          when(mockEclRegistrationService.getRegistrationValidationErrors(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("", None))))

          val result: Future[Result] = controller.onPageLoad()(registrationDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }

  "onSubmit" should {
    "redirect to the registration submitted page after submitting the registration with one contact and sending email successfully" in forAll(
      Arbitrary.arbitrary[CreateEclSubscriptionResponse],
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[RegistrationAdditionalInfo],
      Arbitrary.arbitrary[EntityType].retryUntil(!EntityType.isOther(_)),
      emailAddress(EmailMaxLength)
    ) {
      (
        createEclSubscriptionResponse: CreateEclSubscriptionResponse,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo,
        entityType: EntityType,
        firstContactEmailAddress: String
      ) =>
        val updatedRegistration = registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
          entityType = Some(entityType),
          registrationType = Some(Initial),
          contactAddress = Some(EclAddress.empty),
          contacts = Contacts(
            firstContactDetails = validContactDetails.copy(emailAddress = Some(firstContactEmailAddress)),
            secondContact = Some(false),
            secondContactDetails = ContactDetails.empty
          )
        )
        new TestContext(updatedRegistration, Some(additionalInfo)) {
          when(mockEclRegistrationService.upsertRegistration(any())(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(
            mockEclRegistrationService
              .submitRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any(), any())
          )
            .thenReturn(
              EitherT[Future, DataRetrievalError, CreateEclSubscriptionResponse](
                Future.successful(Right(createEclSubscriptionResponse))
              )
            )

          when(mockEmailService.sendRegistrationSubmittedEmails(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.RegistrationSubmittedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendRegistrationSubmittedEmails(
            ArgumentMatchers.eq(updatedRegistration.contacts),
            ArgumentMatchers.eq(createEclSubscriptionResponse.eclReference),
            ArgumentMatchers.eq(updatedRegistration.entityType),
            any[Option[RegistrationAdditionalInfo]](),
            ArgumentMatchers.eq(updatedRegistration.carriedOutAmlRegulatedActivityInCurrentFy)
          )(any(), any())

          reset(mockEclRegistrationService)
          reset(mockRegistrationAdditionalInfoService)
          reset(mockEmailService)
        }
    }

    "redirect to the registration received page when entity type is other, after submitting the registration with one contact and sending email successfully" in forAll(
      Arbitrary.arbitrary[CreateEclSubscriptionResponse],
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[RegistrationAdditionalInfo],
      Arbitrary.arbitrary[EntityType].retryUntil(EntityType.isOther),
      emailAddress(EmailMaxLength)
    ) {
      (
        createEclSubscriptionResponse: CreateEclSubscriptionResponse,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo,
        entityType: EntityType,
        firstContactEmailAddress: String
      ) =>
        val updatedRegistration = registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
          entityType = Some(entityType),
          registrationType = Some(Initial),
          contactAddress = Some(EclAddress.empty),
          contacts = Contacts(
            firstContactDetails = validContactDetails.copy(emailAddress = Some(firstContactEmailAddress)),
            secondContact = Some(false),
            secondContactDetails = ContactDetails.empty
          )
        )

        new TestContext(updatedRegistration, Some(additionalInfo)) {
          when(mockEclRegistrationService.upsertRegistration(any())(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(
            mockEclRegistrationService
              .submitRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any(), any())
          )
            .thenReturn(EitherT.fromEither[Future](Right(createEclSubscriptionResponse)))

          when(mockEmailService.sendRegistrationSubmittedEmails(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.RegistrationReceivedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendRegistrationSubmittedEmails(
            ArgumentMatchers.eq(updatedRegistration.contacts),
            ArgumentMatchers.eq(createEclSubscriptionResponse.eclReference),
            ArgumentMatchers.eq(updatedRegistration.entityType),
            any[Option[RegistrationAdditionalInfo]](),
            ArgumentMatchers.eq(updatedRegistration.carriedOutAmlRegulatedActivityInCurrentFy)
          )(any(), any())

          val argCaptor: ArgumentCaptor[Registration] = ArgumentCaptor.forClass(classOf[Registration])
          verify(mockEclRegistrationService, times(1))
            .upsertRegistration(argCaptor.capture())(any())
          val submittedRegistration: Registration     = argCaptor.getValue
          submittedRegistration.base64EncodedFields.flatMap(_.dmsSubmissionHtml).getOrElse("").isBlank shouldBe false

          reset(mockEclRegistrationService)
          reset(mockRegistrationAdditionalInfoService)
          reset(mockEmailService)
        }
    }

    "redirect to the registration submitted page after submitting the registration with two contacts and sending emails successfully" in forAll(
      Arbitrary.arbitrary[CreateEclSubscriptionResponse],
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[RegistrationAdditionalInfo],
      Arbitrary.arbitrary[EntityType].retryUntil(!EntityType.isOther(_)),
      emailAddress(EmailMaxLength),
      emailAddress(EmailMaxLength)
    ) {
      (
        createEclSubscriptionResponse: CreateEclSubscriptionResponse,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo,
        entityType: EntityType,
        firstContactEmailAddress: String,
        secondContactEmailAddress: String
      ) =>
        val updatedRegistration = registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
          entityType = Some(entityType),
          registrationType = Some(Initial),
          contactAddress = Some(EclAddress.empty),
          contacts = Contacts(
            firstContactDetails = validContactDetails.copy(emailAddress = Some(firstContactEmailAddress)),
            secondContact = Some(true),
            secondContactDetails = validContactDetails.copy(emailAddress = Some(secondContactEmailAddress))
          )
        )

        new TestContext(updatedRegistration, Some(additionalInfo)) {
          when(mockEclRegistrationService.upsertRegistration(any())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(())))
          when(
            mockEclRegistrationService
              .submitRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any(), any())
          )
            .thenReturn(EitherT.fromEither[Future](Right(createEclSubscriptionResponse)))

          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEmailService.sendRegistrationSubmittedEmails(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.RegistrationSubmittedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendRegistrationSubmittedEmails(
            ArgumentMatchers.eq(updatedRegistration.contacts),
            ArgumentMatchers.eq(createEclSubscriptionResponse.eclReference),
            ArgumentMatchers.eq(updatedRegistration.entityType),
            any[Option[RegistrationAdditionalInfo]](),
            ArgumentMatchers.eq(updatedRegistration.carriedOutAmlRegulatedActivityInCurrentFy)
          )(any(), any())

          reset(mockEclRegistrationService)
          reset(mockRegistrationAdditionalInfoService)
          reset(mockEmailService)
        }
    }

    "redirect to the registration received page when entity type is 'other' after submitting the registration with two contacts and sending emails successfully" in forAll(
      Arbitrary.arbitrary[CreateEclSubscriptionResponse],
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[RegistrationAdditionalInfo],
      Arbitrary.arbitrary[EntityType].retryUntil(EntityType.isOther),
      emailAddress(EmailMaxLength),
      Arbitrary.arbitrary[LiabilityYear]
    ) {
      (
        createEclSubscriptionResponse: CreateEclSubscriptionResponse,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo,
        entityType: EntityType,
        emailAddress: String,
        liabilityYear: LiabilityYear
      ) =>
        val updatedRegistration   = registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
          entityType = Some(entityType),
          registrationType = Some(Initial),
          contactAddress = Some(EclAddress.empty),
          contacts = Contacts(
            firstContactDetails = validContactDetails.copy(emailAddress = Some(emailAddress)),
            secondContact = Some(true),
            secondContactDetails = validContactDetails.copy(emailAddress = Some(emailAddress))
          )
        )
        val updatedAdditionalInfo = additionalInfo.copy(liabilityYear = Some(liabilityYear))

        new TestContext(updatedRegistration, Some(updatedAdditionalInfo)) {
          when(mockEclRegistrationService.upsertRegistration(any())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(())))

          when(
            mockEclRegistrationService
              .submitRegistration(ArgumentMatchers.eq(updatedRegistration.internalId))(any(), any())
          )
            .thenReturn(EitherT.fromEither[Future](Right(createEclSubscriptionResponse)))

          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEmailService.sendRegistrationSubmittedEmails(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.RegistrationReceivedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendRegistrationSubmittedEmails(
            ArgumentMatchers.eq(updatedRegistration.contacts),
            ArgumentMatchers.eq(createEclSubscriptionResponse.eclReference),
            ArgumentMatchers.eq(updatedRegistration.entityType),
            any[Option[RegistrationAdditionalInfo]](),
            ArgumentMatchers.eq(updatedRegistration.carriedOutAmlRegulatedActivityInCurrentFy)
          )(any(), any())

          val argCaptor: ArgumentCaptor[Registration] = ArgumentCaptor.forClass(classOf[Registration])

          verify(mockEclRegistrationService, times(1))
            .upsertRegistration(argCaptor.capture())(any())
          val submittedRegistration: Registration = argCaptor.getValue
          submittedRegistration.base64EncodedFields.flatMap(_.dmsSubmissionHtml).getOrElse("").isBlank shouldBe false

          reset(mockEclRegistrationService)
          reset(mockRegistrationAdditionalInfoService)
          reset(mockEmailService)
        }
    }
  }

  "createAndEncodeHtmlForPdf" should {
    "show liability start date" in forAll(
      Arbitrary.arbitrary[ValidRegistrationWithRegistrationType],
      choose[Int](2022, TaxYear.current.startYear),
      Arbitrary.arbitrary[LocalDate],
      Arbitrary.arbitrary[EntityType].retryUntil(EntityType.isOther)
    ) {
      (
        valid: ValidRegistrationWithRegistrationType,
        year: Int,
        liabilityStartDate: LocalDate,
        entityType: EntityType
      ) =>
        val updatedRegistration = valid.registration.copy(entityType = Some(entityType))
        new TestContext(updatedRegistration) {
          val additionalInfo: RegistrationAdditionalInfo                                        = RegistrationAdditionalInfo(
            valid.registration.internalId,
            Some(LiabilityYear(year)),
            Some(liabilityStartDate),
            None,
            None,
            None
          )
          implicit val registrationDataRequest: RegistrationDataRequest[AnyContentAsEmpty.type] =
            RegistrationDataRequest(
              fakeRequest,
              updatedRegistration.internalId,
              updatedRegistration,
              Some(additionalInfo),
              None
            )

          val encodedHtml: String = controller.encodeOtherRegistrationHtmlForPdf(
            CheckYourAnswersViewModel(
              updatedRegistration,
              None,
              Some(testEclRegistrationReference),
              Some(additionalInfo)
            )
          )(registrationDataRequest)
          val decodedHtml         = new String(Base64.getDecoder.decode(encodedHtml))

          decodedHtml should include(messages("checkYourAnswers.liabilityDate.label"))
          decodedHtml should include(ViewUtils.formatLocalDate(liabilityStartDate)(messages))
        }
    }

  }
}
