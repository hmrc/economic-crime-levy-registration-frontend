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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataAction
import uk.gov.hmrc.economiccrimelevyregistration.forms.deregister.DeregisterDateFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.{arbDeregistration, arbMode}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterDateView

import java.time.LocalDate
import scala.concurrent.Future

class DeregisterDateControllerSpec extends SpecBase {

  val view: DeregisterDateView                 = app.injector.instanceOf[DeregisterDateView]
  val formProvider: DeregisterDateFormProvider = new DeregisterDateFormProvider()

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]

  class TestContext(deregistration: Deregistration) {
    val controller = new DeregisterDateController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(deregistration.internalId),
      fakeDeregistrationDataAction(deregistration),
      mockDeregistrationService,
      formProvider,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { (deregistration: Deregistration, mode: Mode) =>
      new TestContext(deregistration) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

        val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

        val form = deregistration.date match {
          case Some(date) => formProvider().fill(date)
          case None       => formProvider()
        }

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          form,
          mode,
          deregistration.registrationType
        )(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "go to deregistration contact name view" in forAll { (deregistration: Deregistration) =>
      val date = LocalDate.now().minusDays(1)
      new TestContext(deregistration) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

        when(mockDeregistrationService.upsert(any())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration.copy(date = Some(date)))))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(
            fakeRequest.withFormUrlEncodedBody(
              ("value.day", date.getDayOfMonth.toString),
              ("value.month", date.getMonthValue.toString),
              ("value.year", date.getYear.toString)
            )
          )

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DeregisterContactNameController.onPageLoad(NormalMode).url)

        verify(mockDeregistrationService, times(1)).upsert(any())(any())
        reset(mockDeregistrationService)
      }
    }
  }
}
