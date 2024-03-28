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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, GetSubscriptionResponse, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendRegistrationStartView

import scala.concurrent.Future

class AmendRegistrationStartControllerSpec extends SpecBase {

  val view: AmendRegistrationStartView                                         = app.injector.instanceOf[AmendRegistrationStartView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockRegistrationService: EclRegistrationService                          = mock[EclRegistrationService]
  val mockAppConfig: AppConfig                                                 = mock[AppConfig]

  val controller = new AmendRegistrationStartController(
    mcc,
    mockRegistrationAdditionalInfoService,
    fakeAuthorisedActionWithEnrolmentCheck(testInternalId),
    view,
    mockRegistrationService,
    mockAppConfig
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (registration: Registration, getSubscriptionResponse: GetSubscriptionResponse) =>
        val updatedRegistration = registration.copy(registrationType = Some(Amendment))

        when(mockRegistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Registration](Future.successful(Right(registration))))
        when(mockRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))
        when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockRegistrationService.getSubscription(any())(any()))
          .thenReturn(
            EitherT[Future, DataRetrievalError, GetSubscriptionResponse](
              Future.successful(Right(getSubscriptionResponse))
            )
          )
        when(mockRegistrationService.transformToRegistration(any(), any())).thenReturn(updatedRegistration)

        val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

        status(result) shouldBe OK

        reset(mockRegistrationService)
    }

    "route to AmendReasonController when getSubscriptionEnabled is true" in forAll {
      (registration: Registration, getSubscriptionResponse: GetSubscriptionResponse) =>
        val updatedRegistration = registration.copy(registrationType = Some(Amendment))

        when(mockAppConfig.getSubscriptionEnabled).thenReturn(true)

        when(mockRegistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Registration](Future.successful(Right(registration))))
        when(mockRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))
        when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockRegistrationService.getSubscription(any())(any()))
          .thenReturn(
            EitherT[Future, DataRetrievalError, GetSubscriptionResponse](
              Future.successful(Right(getSubscriptionResponse))
            )
          )
        when(mockRegistrationService.transformToRegistration(any(), any())).thenReturn(updatedRegistration)

        val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AmendReasonController.onPageLoad(CheckMode).url)

        reset(mockRegistrationService)
    }

    "return Internal server error and the correct view" in {
      when(mockRegistrationService.getOrCreate(anyString())(any()))
        .thenReturn(
          EitherT[Future, DataRetrievalError, Registration](
            Future.successful(Left(DataRetrievalError.InternalUnexpectedError("", None)))
          )
        )

      val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      reset(mockRegistrationService)
      reset(mockRegistrationAdditionalInfoService)

    }
  }

}
