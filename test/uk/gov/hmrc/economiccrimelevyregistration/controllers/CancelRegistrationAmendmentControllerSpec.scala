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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.forms.CancelRegistrationAmendmentFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CancelRegistrationAmendmentView

import scala.concurrent.Future

class CancelRegistrationAmendmentControllerSpec extends SpecBase {

  val view: CancelRegistrationAmendmentView                 = app.injector.instanceOf[CancelRegistrationAmendmentView]
  val formProvider: CancelRegistrationAmendmentFormProvider = new CancelRegistrationAmendmentFormProvider()
  val form: Form[Boolean]                                   = formProvider()

  def getExpectedValues(cancelRegistrationAmendment: Boolean): (String, Int) =
    if (cancelRegistrationAmendment) {
      (appConfig.yourEclAccountUrl, 1)
    } else {
      (routes.CheckYourAnswersController.onPageLoad().url, 0)
    }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new CancelRegistrationAmendmentController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId, Some(testEclRegistrationReference)),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      appConfig,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, Some(testEclRegistrationReference))(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, cancelRegistrationAmendment: Boolean) =>
        new TestContext(registration) {

          when(mockEclRegistrationConnector.deleteRegistration(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] =
            controller.onSubmit()(
              fakeRequest.withFormUrlEncodedBody(("value", cancelRegistrationAmendment.toString))
            )

          val expected: (String, Int) = getExpectedValues(cancelRegistrationAmendment)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(expected._1)

          verify(mockEclRegistrationConnector, times(expected._2)).deleteRegistration(anyString())(any())

          reset(mockEclRegistrationConnector)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]        = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, Some(testEclRegistrationReference))(
          fakeRequest,
          messages
        ).toString
      }
    }
  }
}
