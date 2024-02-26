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
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.SavedResponsesFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.SavedResponsesView

import scala.concurrent.Future

class SavedResponsesControllerSpec extends SpecBase {

  val view: SavedResponsesView                 = app.injector.instanceOf[SavedResponsesView]
  val formProvider: SavedResponsesFormProvider = new SavedResponsesFormProvider()
  val form: Form[Boolean]                      = formProvider()

  val mockEclRegistrationService: EclRegistrationService           = mock[EclRegistrationService]
  val mockAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockSessionService: SessionService                           = mock[SessionService]

  class TestContext(internalId: String) {
    val controller = new SavedResponsesController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(internalId),
      mockEclRegistrationService,
      mockAdditionalInfoService,
      mockSessionService,
      formProvider,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { internalID: String =>
      new TestContext(internalID) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "continue with saved responses if answer is yes" in forAll { (internalId: String, url: String) =>
      new TestContext(internalId) {
        when(mockSessionService.get(any(), any(), any())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(url)))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "true")))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(url)
      }
    }

    "delete registration if answer is no" in forAll { internalId: String =>
      new TestContext(internalId) {
        when(mockEclRegistrationService.deleteRegistration(any())(any(), any()))
          .thenReturn(EitherT.fromEither[Future](Right(())))

        when(mockAdditionalInfoService.delete(any())(any(), any()))
          .thenReturn(EitherT.fromEither[Future](Right(())))

        when(mockSessionService.delete(any())(any()))
          .thenReturn(EitherT.fromEither[Future](Right()))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "false")))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.RegisterForCurrentYearController.onPageLoad(NormalMode).url)
      }
    }
  }
}
