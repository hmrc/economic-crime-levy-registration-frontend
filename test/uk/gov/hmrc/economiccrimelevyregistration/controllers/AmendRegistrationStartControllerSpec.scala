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

import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendRegistrationStartView

import scala.concurrent.Future

class AmendRegistrationStartControllerSpec extends SpecBase {

  val view: AmendRegistrationStartView                                         = app.injector.instanceOf[AmendRegistrationStartView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockRegistrationConnector: EclRegistrationConnector                      = mock[EclRegistrationConnector]
  val mockRegistrationService: EclRegistrationService                          = mock[EclRegistrationService]
  val mockErrorHandler: ErrorHandler                                           = mock[ErrorHandler]

  val controller = new AmendRegistrationStartController(
    mcc,
    mockRegistrationAdditionalInfoService,
    fakeAuthorisedActionWithEnrolmentCheck(testInternalId),
    mockErrorHandler,
    view,
    mockRegistrationService,
    mockRegistrationConnector,
    appConfig
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (registration: Registration, getSubscriptionResponse: GetSubscriptionResponse) =>
        when(
          mockRegistrationAdditionalInfoService.createOrUpdate(
            anyString(),
            any()
          )(any())
        ).thenReturn(
          Future.successful(())
        )

        when(mockRegistrationService.getOrCreateRegistration(any())(any()))
          .thenReturn(Future.successful(registration))

        when(mockRegistrationConnector.upsertRegistration(any())(any()))
          .thenReturn(Future.successful(registration))

        when(mockRegistrationService.getSubscription(any())(any()))
          .thenReturn(Future.successful(getSubscriptionResponse))

        when(mockRegistrationService.upsertRegistration(any())(any()))
          .thenReturn(Future.successful(registration))

        val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

        status(result) shouldBe SEE_OTHER
    }

    "return Internal server error and the correct view" in forAll { registration: Registration =>
      when(mockRegistrationService.getOrCreateRegistration(any())(any()))
        .thenReturn(Future.successful(registration))

      when(mockRegistrationConnector.upsertRegistration(any())(any()))
        .thenReturn(Future.successful(registration))

      when(
        mockRegistrationAdditionalInfoService.createOrUpdate(
          anyString(),
          any()
        )(any())
      ).thenReturn(
        Future.failed(new Exception("error"))
      )

      when(
        mockErrorHandler.internalServerErrorTemplate(any())
      ).thenReturn(
        Html("error page")
      )

      val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
