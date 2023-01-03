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
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.SecondContactNumberFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.SecondContactNumberPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.SecondContactNumberView

import scala.concurrent.Future

class SecondContactNumberControllerSpec extends SpecBase {

  val view: SecondContactNumberView                 = app.injector.instanceOf[SecondContactNumberView]
  val formProvider: SecondContactNumberFormProvider = new SecondContactNumberFormProvider()
  val form: Form[String]                            = formProvider()

  val pageNavigator: SecondContactNumberPageNavigator = new SecondContactNumberPageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call = onwardRoute
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val numberMaxLength: Int = 24

  class TestContext(registrationData: Registration) {
    val controller = new SecondContactNumberController(
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
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(contacts =
            Contacts.empty.copy(secondContactDetails = ContactDetails(name = Some(name), None, None, None))
          )
        ) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, name)(fakeRequest, messages).toString
        }
    }

    "throw an IllegalStateException when there is no second contact name in the registration data" in forAll {
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
            await(controller.onPageLoad()(fakeRequest))
          }

          result.getMessage shouldBe "No second contact name found in registration data"
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, number: String, name: String) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts
              .copy(secondContactDetails =
                registration.contacts.secondContactDetails.copy(name = Some(name), telephoneNumber = Some(number))
              )
          )
        ) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(number), name)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided contact number then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      telephoneNumber(numberMaxLength)
    ) { (registration: Registration, number: String) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(telephoneNumber = Some(number))
            )
          )

        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", number)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts
              .copy(secondContactDetails = registration.contacts.secondContactDetails.copy(name = Some(name)))
          )
        ) {
          val result: Future[Result]       = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, name)(fakeRequest, messages).toString
        }
    }
  }
}
