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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.FirstContactNameFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.FirstContactNamePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.FirstContactNameView

import scala.concurrent.Future

class FirstContactNameControllerSpec extends SpecBase {

  val view: FirstContactNameView                 = app.injector.instanceOf[FirstContactNameView]
  val formProvider: FirstContactNameFormProvider = new FirstContactNameFormProvider()
  val form: Form[String]                         = formProvider()

  val pageNavigator: FirstContactNamePageNavigator = new FirstContactNamePageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call = onwardRoute
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new FirstContactNameController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(contacts = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, contacts: Contacts) =>
        new TestContext(registration.copy(contacts = Some(contacts))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(contacts.firstContactDetails.name))(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected contact name then redirect to the next page" in forAll {
      (registration: Registration, contacts: Contacts) =>
        new TestContext(registration) {
          val updatedRegistration: Registration = registration.copy(contacts = Some(contacts))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", contacts.firstContactDetails.name)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]       = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
