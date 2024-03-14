/*
 * Copyright 2024 HM Revenue & Customs
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
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.{LiabilityDateAdditionalInfoCleanup, LiabilityDateRegistrationCleanup}
import uk.gov.hmrc.economiccrimelevyregistration.forms.RegisterForCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode, Registration, RegistrationAdditionalInfo, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.RegisterForCurrentYearPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RegisterForCurrentYearView

import scala.concurrent.Future

class RegisterForCurrentYearControllerSpec extends SpecBase {

  val view: RegisterForCurrentYearView = app.injector.instanceOf[RegisterForCurrentYearView]
  val formProvider                     = new RegisterForCurrentYearFormProvider
  val form: Form[Boolean]              = formProvider()

  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockEclRegistrationService: EclRegistrationService                       = mock[EclRegistrationService]
  val mockSessionService: SessionService                                       = mock[SessionService]

  val pageNavigator: RegisterForCurrentYearPageNavigator = new RegisterForCurrentYearPageNavigator() {
    override protected def navigateInNormalMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = routes.EntityTypeController.onPageLoad(NormalMode)

    override protected def navigateInCheckMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = routes.CheckYourAnswersController.onPageLoad()
  }

  val registrationDataCleanup: LiabilityDateRegistrationCleanup = new LiabilityDateRegistrationCleanup() {
    override def cleanup(registration: Registration): Registration = registration
  }

  val additionalInfoDataCleanup: LiabilityDateAdditionalInfoCleanup = new LiabilityDateAdditionalInfoCleanup() {
    override def cleanup(additionalInfo: RegistrationAdditionalInfo): RegistrationAdditionalInfo = additionalInfo
  }

  class TestContext(registrationData: Registration, registrationAdditionalInfo: RegistrationAdditionalInfo) {
    val controller = new RegisterForCurrentYearController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData, Some(registrationAdditionalInfo)),
      mockSessionService,
      formProvider,
      mockRegistrationAdditionalInfoService,
      mockEclRegistrationService,
      pageNavigator,
      registrationDataCleanup,
      additionalInfoDataCleanup,
      view
    )
  }

  "onPageLoad" should {
    "return OK with correct view when no answers has been provided" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration, additionalInfo.copy(registeringForCurrentYear = None)) {
          when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.UrlToReturnTo))(any()))
            .thenReturn(EitherT.fromEither[Future](Right(None)))

          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form,
            NormalMode,
            s"${EclTaxYear.currentStartYear()} to ${EclTaxYear.currentFinishYear()}",
            EclTaxYear.currentFinancialYearStartDate,
            EclTaxYear.currentFinancialYearFinishDate
          )(messages, fakeRequest).toString()
        }
    }

    "return OK with correct view when true as answer has been provided" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.UrlToReturnTo))(any()))
          .thenReturn(EitherT.fromEither[Future](Right(None)))

        new TestContext(registration, additionalInfo.copy(registeringForCurrentYear = Some(true))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(true),
            NormalMode,
            s"${EclTaxYear.currentStartYear()} to ${EclTaxYear.currentFinishYear()}",
            EclTaxYear.currentFinancialYearStartDate,
            EclTaxYear.currentFinancialYearFinishDate
          )(messages, fakeRequest).toString()
        }
    }

    "return OK with correct view when false as answer has been provided" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration, additionalInfo.copy(registeringForCurrentYear = Some(false))) {
          when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.UrlToReturnTo))(any()))
            .thenReturn(EitherT.fromEither[Future](Right(None)))

          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(false),
            NormalMode,
            s"${EclTaxYear.currentStartYear()} to ${EclTaxYear.currentFinishYear()}",
            EclTaxYear.currentFinancialYearStartDate,
            EclTaxYear.currentFinancialYearFinishDate
          )(messages, fakeRequest).toString()
        }
    }

    "redirect to Saved Responses page if there is a return url" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration, additionalInfo.copy(registeringForCurrentYear = Some(false))) {
          when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.UrlToReturnTo))(any()))
            .thenReturn(EitherT.fromEither[Future](Right(Some(random[String]))))

          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.SavedResponsesController.onPageLoad.url)
        }
    }
  }

  "onSubmit" should {
    "save the answer and redirect to the correct page in NormalMode" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration, additionalInfo) {

          when(mockRegistrationAdditionalInfoService.get(any())(any(), any()))
            .thenReturn(
              EitherT[Future, DataRetrievalError, Option[RegistrationAdditionalInfo]](
                Future.successful(Right(Some(additionalInfo)))
              )
            )
          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.upsertRegistration(any())(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "true")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.EntityTypeController.onPageLoad(NormalMode).url)
        }
    }

    "save the answer and redirect to the correct page in CheckMode" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration, additionalInfo) {

          when(mockRegistrationAdditionalInfoService.get(any())(any(), any()))
            .thenReturn(
              EitherT[Future, DataRetrievalError, Option[RegistrationAdditionalInfo]](
                Future.successful(Right(Some(additionalInfo)))
              )
            )
          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.upsertRegistration(any())(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", "true")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
        }
    }
  }
}
