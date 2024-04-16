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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, LocalDateService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RegistrationReceivedView

import scala.concurrent.Future

class RegistrationReceivedControllerSpec extends SpecBase {

  val view: RegistrationReceivedView                                           = app.injector.instanceOf[RegistrationReceivedView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockEclRegistrationService: EclRegistrationService                       = mock[EclRegistrationService]
  val mockLocalDateService: LocalDateService                                   = mock[LocalDateService]

  when(mockLocalDateService.now()).thenReturn(testCurrentDate)

  val controller = new RegistrationReceivedController(
    mcc,
    fakeAuthorisedActionWithoutEnrolmentCheck(testInternalId),
    view,
    mockRegistrationAdditionalInfoService,
    mockEclRegistrationService,
    mockLocalDateService
  )

  "onPageLoad" should {
    "return OK and the correct view when there is one contact email address, aml activity and a liability year in the session" in forAll {
      (liabilityYear: LiabilityYear, firstContactEmailAddress: String) =>
        val registeringForCurrentYear = true

        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.firstContactEmail       -> firstContactEmailAddress,
            SessionKeys.registeringForCurrentFY -> registeringForCurrentYear.toString,
            SessionKeys.liabilityYear           -> liabilityYear.asString
          )
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          firstContactEmailAddress,
          None,
          Some(liabilityYear),
          registeringForCurrentYear,
          testEclTaxYear
        )(fakeRequest, messages).toString
    }

    "return OK and the correct view when there are two contact email addresses, aml activity and a liability year in the session" in forAll {
      (liabilityYear: LiabilityYear, firstContactEmailAddress: String, secondContactEmailAddress: String) =>
        val registeringForCurrentYear = true
        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.firstContactEmail       -> firstContactEmailAddress,
            SessionKeys.secondContactEmail      -> secondContactEmailAddress,
            SessionKeys.registeringForCurrentFY -> registeringForCurrentYear.toString,
            SessionKeys.liabilityYear           -> liabilityYear.asString
          )
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          firstContactEmailAddress,
          Some(secondContactEmailAddress),
          Some(liabilityYear),
          registeringForCurrentYear,
          testEclTaxYear
        )(fakeRequest, messages).toString
    }

    "return an Internal Server Error when no first contact email is present in the session" in forAll {
      (liabilityYear: LiabilityYear) =>
        val registeringForCurrentYear = true

        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.registeringForCurrentFY -> registeringForCurrentYear.toString,
            SessionKeys.liabilityYear           -> liabilityYear.asString
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR

    }

    "return an Internal Server Error when no amlRegulatedActivity answer is present in the session" in forAll {
      (liabilityYear: LiabilityYear, firstContactEmailAddress: String) =>
        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.firstContactEmail -> firstContactEmailAddress,
            SessionKeys.liabilityYear     -> liabilityYear.asString
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR

    }

    "return an Internal Server Error when no liability year is present in the session" in forAll {
      firstContactEmailAddress: String =>
        val registeringForCurrentYear = true

        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.firstContactEmail       -> firstContactEmailAddress,
            SessionKeys.registeringForCurrentFY -> registeringForCurrentYear.toString
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR

    }

    "return a DataRetrievalError when call to delete additional info fails" in forAll {
      (firstContactEmailAddress: String, liabilityYear: LiabilityYear) =>
        val registeringForCurrentYear = true

        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("", None))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.firstContactEmail       -> firstContactEmailAddress,
            SessionKeys.registeringForCurrentFY -> registeringForCurrentYear.toString,
            SessionKeys.liabilityYear           -> liabilityYear.asString
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR

    }

    "return a DataRetrievalError when call to delete registration fails" in forAll {
      (firstContactEmailAddress: String, liabilityYear: LiabilityYear) =>
        val registeringForCurrentYear = true

        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("", None))))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.firstContactEmail       -> firstContactEmailAddress,
            SessionKeys.registeringForCurrentFY -> registeringForCurrentYear.toString,
            SessionKeys.liabilityYear           -> liabilityYear.asString
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR

    }

  }

}
