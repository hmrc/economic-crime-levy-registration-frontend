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
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.StartView

import scala.concurrent.Future

class StartControllerSpec extends SpecBase {

  val mockSessionService: SessionService = mock[SessionService]

  val view: StartView = app.injector.instanceOf[StartView]

  val controller = new StartController(
    mcc,
    fakeAuthorisedActionWithEnrolmentCheck(testInternalId),
    mockSessionService,
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in {
      val result: Future[Result] = controller.onPageLoad()(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe view()(fakeRequest, messages).toString
    }
  }

  "onSubmit" should {
    "redirect to register for current year page if no return url" in {
      when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.UrlToReturnTo))(any()))
        .thenReturn(EitherT.fromEither[Future](Right(None)))

      val result: Future[Result] = controller.onSubmit()(fakeRequest)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RegisterForCurrentYearController.onPageLoad(NormalMode).url)
    }

    "redirect to Saved Responses page if there is a return url" in {
      when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.UrlToReturnTo))(any()))
        .thenReturn(EitherT.fromEither[Future](Right(Some(random[String]))))

      val result: Future[Result] = controller.onSubmit()(fakeRequest)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.SavedResponsesController.onPageLoad.url)
    }
  }
}
