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
import uk.gov.hmrc.economiccrimelevyregistration.forms.ExampleCheckBoxFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{ExampleCheckBox, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ExampleCheckBoxView

import scala.concurrent.Future

class ExampleCheckBoxControllerSpec extends SpecBase {

  val view: ExampleCheckBoxView = app.injector.instanceOf[ExampleCheckBoxView]

  val formProvider = new ExampleCheckBoxFormProvider()

  val form: Form[Set[ExampleCheckBox]] = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestFixture(data: Registration = emptyRegistration) {
    val controller = new ExampleCheckBoxController(
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
      emptyRegistration.copy(exampleCheckBox = Some(ExampleCheckBox.values.toSet))
    ) {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form.fill(ExampleCheckBox.values.toSet), NormalMode)(
        fakeRequest,
        messages
      ).toString
    }

    "redirect to the next page when valid data is submitted" in new TestFixture() {
      when(mockEclRegistrationConnector.updateRegistration(any())).thenReturn(Future.successful(emptyRegistration))

      val result: Future[Result] =
        controller.onSubmit(NormalMode)(
          fakeRequest.withFormUrlEncodedBody("value[0]" -> ExampleCheckBox.values.head.toString)
        )

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe onwardRoute.url
    }

    "return a Bad Request and errors when invalid data is submitted" in new TestFixture() {
      val result: Future[Result] = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value[0]" -> ""))

      val formWithErrors: Form[Set[ExampleCheckBox]] = form.bind(Map("value[0]" -> ""))

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
    }
  }
}
