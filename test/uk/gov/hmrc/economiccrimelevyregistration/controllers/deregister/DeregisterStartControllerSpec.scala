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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.arbGetSubscriptionResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.DeRegistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterStartView

import scala.concurrent.Future

class DeregisterStartControllerSpec extends SpecBase {

  val view: DeregisterStartView = app.injector.instanceOf[DeregisterStartView]

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val e = new Exception("error")

  class TestContext(internalId: String, eclReference: String) {
    val controller = new DeregisterStartController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(internalId, Some(eclReference)),
      mockEclRegistrationService,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (subscription: GetSubscriptionResponse, internalId: String, eclReference: String) =>
        new TestContext(internalId, eclReference) {
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            eclReference,
            subscription.legalEntityDetails.organisationName,
            DeRegistration
          )(fakeRequest, messages).toString
        }
    }

    "return error page if failure" in forAll {
      (subscription: GetSubscriptionResponse, internalId: String, eclReference: String) =>
        new TestContext(internalId, eclReference) {
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(
              EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError(e.getMessage, Some(e))))
            )

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR

          contentAsString(result) should not be view(
            eclReference,
            subscription.legalEntityDetails.organisationName,
            DeRegistration
          )(fakeRequest, messages).toString
        }
    }
  }

}
