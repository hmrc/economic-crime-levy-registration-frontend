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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts

import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.FirstContactEmailFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.FirstContactEmailPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.FirstContactEmailView

import scala.concurrent.Future

class FirstContactEmailControllerSpec extends SpecBase {

  val view: FirstContactEmailView                 = app.injector.instanceOf[FirstContactEmailView]
  val formProvider: FirstContactEmailFormProvider = new FirstContactEmailFormProvider()
  val form: Form[String]                          = formProvider()

  val pageNavigator: FirstContactEmailPageNavigator = new FirstContactEmailPageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call = onwardRoute
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val emailMaxLength: Int = 160

  class TestContext(registrationData: Registration) {
    val controller = new FirstContactEmailController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(contacts =
            Contacts.empty.copy(firstContactDetails = ContactDetails(name = Some(name), None, None, None))
          )
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, name, NormalMode)(fakeRequest, messages).toString
        }
    }

    "throw an IllegalStateException when there is no first contact name in the registration data" in forAll {
      (
        registration: Registration
      ) =>
        val updatedRegistration = registration.copy(
          contacts = Contacts.empty
        )

        new TestContext(
          updatedRegistration
        ) {
          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onPageLoad(NormalMode)(fakeRequest))
          }

          result.getMessage shouldBe "No first contact name found in registration data"
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, name: String, email: String) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts
              .copy(firstContactDetails =
                registration.contacts.firstContactDetails.copy(name = Some(name), emailAddress = Some(email))
              )
          )
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(email), name, NormalMode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided contact email then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      emailAddress(emailMaxLength)
    ) { (registration: Registration, email: String) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(firstContactDetails =
              registration.contacts.firstContactDetails.copy(emailAddress = Some(email))
            )
          )

        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", email)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts
              .copy(firstContactDetails = registration.contacts.firstContactDetails.copy(name = Some(name)))
          )
        ) {
          val result: Future[Result]       =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, name, NormalMode)(fakeRequest, messages).toString
        }
    }
  }
}
