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
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.SecondContactNameFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.SecondContactNamePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.SecondContactNameView

import scala.concurrent.Future

class SecondContactNameControllerSpec extends SpecBase {

  val view: SecondContactNameView                 = app.injector.instanceOf[SecondContactNameView]
  val formProvider: SecondContactNameFormProvider = new SecondContactNameFormProvider()
  val form: Form[String]                          = formProvider()

  val pageNavigator: SecondContactNamePageNavigator = new SecondContactNamePageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call = onwardRoute
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val nameMaxLength: Int = 160

  class TestContext(registrationData: Registration) {
    val controller = new SecondContactNameController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck,
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(
        registration.copy(contacts = Contacts.empty)
      ) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts
              .copy(secondContactDetails = registration.contacts.secondContactDetails.copy(name = Some(name)))
          )
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(name), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided contact name then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(160)
    ) { (registration: Registration, name: String) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(name = Some(name))
            )
          )

        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", name)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]       = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
