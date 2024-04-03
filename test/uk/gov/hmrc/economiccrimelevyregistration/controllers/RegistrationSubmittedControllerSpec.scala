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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, Registration, RegistrationAdditionalInfo, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}

import scala.concurrent.Future

class RegistrationSubmittedControllerSpec extends SpecBase {

  val view: RegistrationSubmittedView                                              = app.injector.instanceOf[RegistrationSubmittedView]
  val outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView =
    app.injector.instanceOf[OutOfSessionRegistrationSubmittedView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService     = mock[RegistrationAdditionalInfoService]
  val mockEclRegistrationService: EclRegistrationService                           = mock[EclRegistrationService]

  class TestContext(registrationData: Registration, additionalInfo: Option[RegistrationAdditionalInfo] = None) {
    val controller = new RegistrationSubmittedController(
      mcc,
      fakeAuthorisedActionWithoutEnrolmentCheck(testInternalId),
      fakeRegistrationDataAction(registrationData, additionalInfo),
      view,
      outOfSessionRegistrationSubmittedView,
      mockRegistrationAdditionalInfoService,
      mockEclRegistrationService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when there is one contact email address and aml activity in the session" in forAll {
      (
        liabilityYear: LiabilityYear,
        email: String,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo
      ) =>
        new TestContext(registration, Some(additionalInfo)) {
          when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(
              SessionKeys.EclReference      -> testEclRegistrationReference,
              SessionKeys.FirstContactEmail -> email,
              SessionKeys.LiabilityYear     -> liabilityYear.asString
            )
          )

          status(result) shouldBe OK

        }
    }

    "redirect to the answers are invalid page when there is no first contact email present in the session" in forAll {
      (
        liabilityYear: LiabilityYear,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo
      ) =>
        new TestContext(registration, Some(additionalInfo)) {
          when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(
              SessionKeys.EclReference  -> testEclRegistrationReference,
              SessionKeys.LiabilityYear -> liabilityYear.asString
            )
          )

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
        }
    }

    "redirect to the answers are invalid page when there is no liability year present in the session" in forAll {
      (
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo,
        email: String
      ) =>
        new TestContext(registration, Some(additionalInfo)) {
          when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(
              SessionKeys.EclReference      -> testEclRegistrationReference,
              SessionKeys.FirstContactEmail -> email
            )
          )

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
        }
    }

    "redirect to the answers are invalid page when there is no ecl Reference number in either the request or the session" in forAll {
      (
        liabilityYear: LiabilityYear,
        email: String,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo
      ) =>
        val updatedAdditionalInfo = additionalInfo.copy(eclReference = None)

        new TestContext(registration, Some(updatedAdditionalInfo)) {
          when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(
              SessionKeys.FirstContactEmail -> email,
              SessionKeys.LiabilityYear     -> liabilityYear.asString
            )
          )

          redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
        }
    }

    "redirect to the outOfSessionRegistrationSubmitted page when there is no ecl Reference number in either the request but it is present in the session" in forAll {
      (
        liabilityYear: LiabilityYear,
        email: String,
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo
      ) =>
        val updatedAdditionalInfo = additionalInfo.copy(eclReference = None)

        new TestContext(registration, Some(updatedAdditionalInfo)) {
          when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(
              SessionKeys.FirstContactEmail -> email,
              SessionKeys.EclReference      -> testEclRegistrationReference,
              SessionKeys.LiabilityYear     -> liabilityYear.asString
            )
          )

          status(result)           shouldBe OK
          contentAsString(result) shouldNot include("Registration Submitted")
          contentAsString(result)    should include(testEclRegistrationReference)

        }
    }

  }

}
