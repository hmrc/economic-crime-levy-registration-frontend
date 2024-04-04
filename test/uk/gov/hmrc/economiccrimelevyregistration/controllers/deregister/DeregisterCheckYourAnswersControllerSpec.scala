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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.scalacheck.Arbitrary
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.{arbDeregistration, arbGetSubscriptionResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, GetSubscriptionResponse, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EmailService}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.{DeregisterCheckYourAnswersView, DeregistrationPdfView}

import scala.concurrent.Future

class DeregisterCheckYourAnswersControllerSpec extends SpecBase {

  val view: DeregisterCheckYourAnswersView = app.injector.instanceOf[DeregisterCheckYourAnswersView]
  val pdfView: DeregistrationPdfView       = app.injector.instanceOf[DeregistrationPdfView]

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockDeregistrationService: DeregistrationService   = mock[DeregistrationService]
  val mockEmailService: EmailService                     = mock[EmailService]

  class TestContext(
    internalId: String,
    eclReference: Option[String],
    deregistration: Deregistration
  ) {
    val controller = new DeregisterCheckYourAnswersController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(internalId, eclReference),
      fakeDeregistrationDataOrErrorAction(deregistration),
      mockEclRegistrationService,
      mockDeregistrationService,
      mockEmailService,
      view,
      pdfView
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (subscription: GetSubscriptionResponse, deregistration: Deregistration) =>
        new TestContext(deregistration.internalId, Some(testEclRegistrationReference), deregistration) {
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))

          when(mockDeregistrationService.upsert(any())(any()))
            .thenReturn(
              EitherT.fromEither[Future](Right(deregistration.copy(eclReference = Some(testEclRegistrationReference))))
            )

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            controller.organisation(
              Some(testEclRegistrationReference),
              subscription.legalEntityDetails.organisationName
            )(messages),
            controller.additionalInfo(deregistration)(messages),
            controller.contact(deregistration.contactDetails)(messages)
          )(fakeRequest, messages).toString

          verify(mockDeregistrationService, times(1)).upsert(any())(any())
          reset(mockDeregistrationService)
        }
    }

    "return an internal server error when no Ecl Reference number is present" in forAll {
      (deregistration: Deregistration) =>
        new TestContext(testInternalId, None, deregistration) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return an internal server error when getSubscription returns a DataRetrievalError" in forAll {
      deregistration: Deregistration =>
        new TestContext(testInternalId, Some(testEclRegistrationReference), deregistration) {
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("", None))))

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return an internal server error when upserting the deregistration fails" in forAll {
      (deregistration: Deregistration, subscription: GetSubscriptionResponse) =>
        new TestContext(testInternalId, Some(testEclRegistrationReference), deregistration) {
          when(mockEclRegistrationService.getSubscription(ArgumentMatchers.eq(testEclRegistrationReference))(any()))
            .thenReturn(
              EitherT[Future, DataRetrievalError, GetSubscriptionResponse](Future.successful(Right(subscription)))
            )
          when(
            mockDeregistrationService.upsert(
              ArgumentMatchers.eq(deregistration.copy(eclReference = Some(testEclRegistrationReference)))
            )(any())
          )
            .thenReturn(
              EitherT[Future, DataRetrievalError, Unit](
                Future.successful(Left(DataRetrievalError.InternalUnexpectedError("", None)))
              )
            )

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }

  "onSubmit" should {
    "go to the deregistration requested page after submitting the deregistration request and sending the email successfully" in forAll {
      (
        deregistration: Deregistration,
        subscription: GetSubscriptionResponse,
        email: String,
        name: String
      ) =>
        val validDeregistration: Deregistration =
          deregistration.copy(
            eclReference = Some(testEclRegistrationReference),
            contactDetails = ContactDetails(Some(name), None, Some(email), None)
          )
        new TestContext(testInternalId, Some(testEclRegistrationReference), validDeregistration) {

          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))

          when(mockDeregistrationService.upsert(any())(any()))
            .thenReturn(
              EitherT.fromEither[Future](Right(deregistration.copy(eclReference = Some(testEclRegistrationReference))))
            )

          when(
            mockEmailService.sendDeregistrationEmail(
              ArgumentMatchers.eq(email),
              ArgumentMatchers.eq(name),
              ArgumentMatchers.eq(testEclRegistrationReference),
              ArgumentMatchers.eq(subscription.correspondenceAddressDetails)
            )(any(), any())
          )
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockDeregistrationService.submit(any())(any())).thenReturn(
            EitherT[Future, DataRetrievalError, Unit](
              Future.successful(Right(()))
            )
          )

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.DeregistrationRequestedController.onPageLoad().url)

          session(result).get(SessionKeys.FirstContactEmail) shouldBe Some(email)
        }
    }

    "return an internal server error when no Ecl Reference number is present" in forAll {
      (deregistration: Deregistration) =>
        new TestContext(testInternalId, None, deregistration) {

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return an internal server error when getSubscription returns a DataRetrievalError" in forAll {
      (deregistration: Deregistration) =>
        new TestContext(testInternalId, Some(testEclRegistrationReference), deregistration) {
          when(mockDeregistrationService.getOrCreate(ArgumentMatchers.eq(testInternalId))(any()))
            .thenReturn(EitherT.fromEither[Future](Right(deregistration)))
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("", None))))

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return an internal server error when no name is present in the deregistration contact details" in forAll {
      (deregistration: Deregistration, subscription: GetSubscriptionResponse) =>
        val updatedDeregistration: Deregistration = deregistration
          .copy(
            contactDetails = ContactDetails(None, None, None, None)
          )
        new TestContext(testInternalId, Some(testEclRegistrationReference), updatedDeregistration) {

          when(mockDeregistrationService.upsert(any())(any()))
            .thenReturn(
              EitherT.fromEither[Future](Right(deregistration.copy(eclReference = Some(testEclRegistrationReference))))
            )

          when(mockDeregistrationService.submit(any())(any())).thenReturn(
            EitherT[Future, DataRetrievalError, Unit](
              Future.successful(Right(()))
            )
          )

          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return an internal server error when no email is present in the deregistration contact details" in forAll {
      (deregistration: Deregistration, subscription: GetSubscriptionResponse) =>
        val updatedDeregistration: Deregistration = deregistration
          .copy(
            contactDetails = ContactDetails(None, None, None, None)
          )
        new TestContext(testInternalId, Some(testEclRegistrationReference), updatedDeregistration) {
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return an internal server error when the email does not send successfully" in forAll {
      (
        deregistration: Deregistration,
        subscription: GetSubscriptionResponse,
        email: String,
        name: String
      ) =>
        val validDeregistration: Deregistration =
          deregistration.copy(
            eclReference = Some(testEclRegistrationReference),
            contactDetails = ContactDetails(Some(name), None, Some(email), None)
          )
        new TestContext(testInternalId, Some(testEclRegistrationReference), validDeregistration) {
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))
          when(
            mockEmailService.sendDeregistrationEmail(
              ArgumentMatchers.eq(email),
              ArgumentMatchers.eq(name),
              ArgumentMatchers.eq(testEclRegistrationReference),
              ArgumentMatchers.eq(subscription.correspondenceAddressDetails)
            )(any(), any())
          )
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.DeregistrationRequestedController.onPageLoad().url)
        }
    }

    "return internal server error when the ecl reference is not present in the request" in forAll {
      (deregistration: Deregistration) =>
        new TestContext(deregistration.internalId, None, deregistration) {

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }
}
