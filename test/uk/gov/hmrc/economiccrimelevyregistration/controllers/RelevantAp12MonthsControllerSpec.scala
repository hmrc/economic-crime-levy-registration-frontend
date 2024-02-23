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
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.RelevantAp12MonthsDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.forms.RelevantAp12MonthsFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.RelevantAp12MonthsPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RelevantAp12MonthsView

import scala.concurrent.Future

class RelevantAp12MonthsControllerSpec extends SpecBase {

  val view: RelevantAp12MonthsView                 = app.injector.instanceOf[RelevantAp12MonthsView]
  val formProvider: RelevantAp12MonthsFormProvider = new RelevantAp12MonthsFormProvider()
  val form: Form[Boolean]                          = formProvider()

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val pageNavigator: RelevantAp12MonthsPageNavigator = new RelevantAp12MonthsPageNavigator {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute
  }

  val dataCleanup: RelevantAp12MonthsDataCleanup = new RelevantAp12MonthsDataCleanup {
    override def cleanup(registration: Registration): Registration = registration
  }

  class TestContext(registrationData: Registration) {
    val controller = new RelevantAp12MonthsController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      dataCleanup,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(relevantAp12Months = None)) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, relevantAp12Months: Boolean) =>
        new TestContext(
          registration.copy(relevantAp12Months = Some(relevantAp12Months))
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(relevantAp12Months), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, relevantAp12Months: Boolean) =>
        new TestContext(registration) {
          val updatedRegistration: Registration =
            registration.copy(relevantAp12Months = Some(relevantAp12Months))

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
              fakeRequest.withFormUrlEncodedBody(("value", relevantAp12Months.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]        = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
