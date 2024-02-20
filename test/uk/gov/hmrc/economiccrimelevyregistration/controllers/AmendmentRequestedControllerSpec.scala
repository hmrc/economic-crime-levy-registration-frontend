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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, SessionError}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendmentRequestedView

import scala.concurrent.Future

class AmendmentRequestedControllerSpec extends SpecBase {

  val view: AmendmentRequestedView                    = app.injector.instanceOf[AmendmentRequestedView]
  val mockSessionService: SessionService              = mock[SessionService]
  val mockRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockAdditionalInfoService                       = mock[RegistrationAdditionalInfoService]

  val controller = new AmendmentRequestedController(
    mcc,
    view,
    fakeAuthorisedActionWithEnrolmentCheck("test-internal-id"),
    mockSessionService,
    mockAdditionalInfoService,
    mockRegistrationService
  )

  "onPageLoad" should {
    "return OK and the correct view when there is one contact email address and aml activity in the session" in forAll {
      (
        eclAddress: EclAddress,
        firstContactEmailAddress: String
      ) =>
        val json = Json.toJson(eclAddress).toString()

        val request = fakeRequest.withSession(
          (SessionKeys.ContactAddress, json),
          (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress)
        )
        when(mockAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(
          mockSessionService.get(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.FirstContactEmailAddress)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(firstContactEmailAddress))))

        when(
          mockSessionService.get(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.ContactAddress)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(json))))

        val result: Future[Result] = controller.onPageLoad()(
          request
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          firstContactEmailAddress,
          None,
          Some(eclAddress)
        )(fakeRequest, messages).toString
    }
  }
}
