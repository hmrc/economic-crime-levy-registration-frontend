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
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregistrationRequestedView
import org.mockito.Mockito.when

import scala.concurrent.Future

class DeregistrationRequestedControllerSpec extends SpecBase {

  val view: DeregistrationRequestedView                  = app.injector.instanceOf[DeregistrationRequestedView]
  val mockSessionService: SessionService                 = mock[SessionService]
  val mockDeregistrationService: DeregistrationService   = mock[DeregistrationService]
  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  class TestContext(deregistration: Deregistration, eclReference: Option[String]) {
    val controller = new DeregistrationRequestedController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck("test-internal-id", eclReference),
      mockDeregistrationService,
      mockEclRegistrationService,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when the call to get subscription succeeds" in forAll {
      (
        email: String,
        subscriptionResponse: GetSubscriptionResponse,
        deregistration: Deregistration
      ) =>
        new TestContext(deregistration, Some(testEclRegistrationReference)) {
          when(mockDeregistrationService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(
              EitherT[Future, DataRetrievalError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(SessionKeys.firstContactEmail -> email)
          )

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            testEclRegistrationReference,
            email,
            subscriptionResponse.correspondenceAddressDetails
          )(fakeRequest, messages).toString
        }
    }

    "return InternalServerError when the call to get subscription fails" in forAll {
      (
        email: String,
        subscriptionResponse: GetSubscriptionResponse,
        deregistration: Deregistration
      ) =>
        new TestContext(deregistration, None) {
          when(mockDeregistrationService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(
              EitherT[Future, DataRetrievalError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(SessionKeys.firstContactEmail -> email)
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError when the eclReference is not available in the request object" in forAll {
      (
        email: String,
        deregistration: Deregistration
      ) =>
        new TestContext(deregistration, None) {
          when(mockDeregistrationService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(SessionKeys.firstContactEmail -> email)
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError when the email address is not present on the session" in forAll {
      (
        deregistration: Deregistration
      ) =>
        new TestContext(deregistration, None) {
          when(mockDeregistrationService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }

}
