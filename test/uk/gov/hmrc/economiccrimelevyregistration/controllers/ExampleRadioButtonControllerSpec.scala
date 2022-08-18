/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.forms.ExampleRadioButtonFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{ExampleRadioButton, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ExampleRadioButtonView

import scala.concurrent.Future

class ExampleRadioButtonControllerSpec extends SpecBase {

  val view: ExampleRadioButtonView = app.injector.instanceOf[ExampleRadioButtonView]

  val formProvider = new ExampleRadioButtonFormProvider()

  val form: Form[ExampleRadioButton] = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestFixture(data: Registration = emptyRegistration) {
    val controller = new ExampleRadioButtonController(
      messagesApi = messagesApi,
      eclRegistrationConnector = mockEclRegistrationConnector,
      navigator = fakeNavigator,
      authorise = fakeAuthorisedAction,
      getRegistrationData = fakeDataRetrievalAction(data),
      formProvider = formProvider,
      controllerComponents = mcc,
      view = view
    )
  }

  "onPageLoad" should {

    "return OK and the correct view" in new TestFixture() {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
    }

    "populate the view correctly when the question has previously been answered" in new TestFixture(
      emptyRegistration.copy(exampleRadioButton = Some(ExampleRadioButton.values.head))
    ) {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form.fill(ExampleRadioButton.values.head), NormalMode)(
        fakeRequest,
        messages
      ).toString
    }

    "redirect to the next page when valid data is submitted" in new TestFixture() {
      when(mockEclRegistrationConnector.updateRegistration(any())).thenReturn(Future.successful(emptyRegistration))

      val result: Future[Result] =
        controller.onSubmit(NormalMode)(
          fakeRequest.withFormUrlEncodedBody("value" -> ExampleRadioButton.values.head.toString)
        )

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe onwardRoute.url
    }

    "return a Bad Request and errors when invalid data is submitted" in new TestFixture() {
      val result: Future[Result] = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> ""))

      val formWithErrors: Form[ExampleRadioButton] = form.bind(Map("value" -> ""))

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
    }
  }
}
