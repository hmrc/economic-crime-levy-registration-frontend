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
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status._
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityDateFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.navigation.LiabilityDatePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{LocalDateService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.LiabilityDateView
import org.mockito.Mockito.{reset, when}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import java.time.LocalDate
import scala.concurrent.Future

class LiabilityDateControllerSpec extends SpecBase {

  val mockLocalDateService: LocalDateService = mock[LocalDateService]
  when(mockLocalDateService.now()).thenReturn(testCurrentDate)

  val formProvider          = new LiabilityDateFormProvider()
  val form: Form[LocalDate] = formProvider(mockLocalDateService)

  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val view: LiabilityDateView                                                  = app.injector.instanceOf[LiabilityDateView]

  val pageNavigator: LiabilityDatePageNavigator = new LiabilityDatePageNavigator(
  ) {
    override protected def navigateInNormalMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = routes.EntityTypeController.onPageLoad(NormalMode)

    override protected def navigateInCheckMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = routes.CheckYourAnswersController.onPageLoad()
  }

  class TestContext(registrationData: Registration, additionalInfo: Option[RegistrationAdditionalInfo] = None) {
    val controller = new LiabilityDateController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData, additionalInfo),
      fakeStoreUrlAction(),
      mockRegistrationAdditionalInfoService,
      formProvider,
      pageNavigator,
      view,
      mockLocalDateService
    )
  }

  "onPageLoad" should {
    "return OK with form not populated because of additional information is missing" in forAll {
      (registration: Registration) =>
        new TestContext(registration, None) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(NormalMode, form)(
            fakeRequest,
            messages
          ).toString
        }
    }

    "return OK with form populated with today's date" in forAll {
      (registration: Registration, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration, Some(additionalInfo.copy(liabilityStartDate = Some(testCurrentDate)))) {
          reset(mockLocalDateService)
          when(mockLocalDateService.now()).thenReturn(testCurrentDate)

          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(NormalMode, form.prepare(Some(testCurrentDate)))(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "return BAD_REQUEST and view with errors when no date has been passed in" in forAll {
      (registration: Registration) =>
        new TestContext(registration, None) {
          val result: Future[Result] = controller.onSubmit(NormalMode)(fakeRequest)

          status(result) shouldBe BAD_REQUEST
        }
    }

    "return OK and redirect to next page in Normal mode when correct date has been passed in" in forAll {
      (registration: Registration, additionalInformation: RegistrationAdditionalInfo) =>
        new TestContext(registration, Some(additionalInformation.copy(liabilityStartDate = Some(testCurrentDate)))) {

          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
              fakeRequest.withFormUrlEncodedBody(
                ("value.day", testCurrentDate.getDayOfMonth.toString),
                ("value.month", testCurrentDate.getMonthValue.toString),
                ("value.year", testCurrentDate.getYear.toString)
              )
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.EntityTypeController.onPageLoad(NormalMode).url)
        }
    }

    "return OK and redirect to next page in Check mode when correct date has been passed in" in forAll {
      (registration: Registration, additionalInformation: RegistrationAdditionalInfo) =>
        val date = testCurrentDate
        new TestContext(registration, Some(additionalInformation.copy(liabilityStartDate = Some(date)))) {

          when(mockRegistrationAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(
              fakeRequest.withFormUrlEncodedBody(
                ("value.day", date.getDayOfMonth.toString),
                ("value.month", date.getMonthValue.toString),
                ("value.year", date.getYear.toString)
              )
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
        }
    }
  }
}
