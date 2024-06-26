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
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityBeforeCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, LocalDateService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.LiabilityBeforeCurrentYearView

import scala.concurrent.Future

class LiabilityBeforeCurrentYearControllerSpec extends SpecBase {

  val view: LiabilityBeforeCurrentYearView                 = app.injector.instanceOf[LiabilityBeforeCurrentYearView]
  val formProvider: LiabilityBeforeCurrentYearFormProvider = new LiabilityBeforeCurrentYearFormProvider()
  val form: Form[Boolean]                                  = formProvider()

  val mockAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockAuditService: AuditService                               = mock[AuditService]
  val mockLocalDateService: LocalDateService                       = mock[LocalDateService]

  when(mockLocalDateService.now()).thenReturn(testCurrentDate)

  class TestContext(registrationData: Registration, additionalInfo: Option[RegistrationAdditionalInfo] = None) {
    val controller = new LiabilityBeforeCurrentYearController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData, additionalInfo),
      fakeStoreUrlAction(),
      formProvider,
      mockAdditionalInfoService,
      view,
      mockAuditService,
      mockLocalDateService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration) =>
        val info: RegistrationAdditionalInfo =
          RegistrationAdditionalInfo(
            registration.internalId,
            None,
            None,
            None,
            None,
            None
          )
        new TestContext(registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = None), Some(info)) {

          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form,
            NormalMode,
            testEclTaxYear
          )(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, liableBeforeCurrentYear: Boolean) =>
        new TestContext(
          registration.copy(
            revenueMeetsThreshold = Some(true),
            businessSector = None
          )
        ) {

          when(mockAdditionalInfoService.upsert(any())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val nextPage: Call = if (liableBeforeCurrentYear) {
            routes.LiabilityDateController.onPageLoad(NormalMode)
          } else {
            routes.EntityTypeController.onPageLoad(NormalMode)
          }

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
              fakeRequest.withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(nextPage.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]        =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(
          formWithErrors,
          NormalMode,
          testEclTaxYear
        )(fakeRequest, messages).toString
      }
    }
  }
}
