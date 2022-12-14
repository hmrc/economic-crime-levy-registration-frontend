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

import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{AddressLookupFrontendConnector, EclRegistrationConnector}
import uk.gov.hmrc.economiccrimelevyregistration.forms.IsUkAddressFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.IsUkAddressPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.IsUkAddressView

import scala.concurrent.Future

class IsUkAddressControllerSpec extends SpecBase {

  val view: IsUkAddressView                 = app.injector.instanceOf[IsUkAddressView]
  val formProvider: IsUkAddressFormProvider = new IsUkAddressFormProvider()
  val form: Form[Boolean]                   = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val pageNavigator: IsUkAddressPageNavigator = new IsUkAddressPageNavigator(
    mock[AddressLookupFrontendConnector]
  ) {
    override protected def navigateInNormalMode(registration: Registration)(implicit
      request: RequestHeader
    ): Future[Call]                                             = Future.successful(onwardRoute)
    override def previousPage(registration: Registration): Call = backRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new IsUkAddressController(
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
      (
        registration: Registration,
      ) =>
        val updatedRegistration = registration.copy(contactAddressIsUk = None)

        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, backRoute.url)(
            fakeRequest,
            messages
          ).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (
        registration: Registration,
        contactAddressIsUk: Boolean
      ) =>
        val updatedRegistration = registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(
            form.fill(contactAddressIsUk),
            backRoute.url
          )(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, contactAddressIsUk: Boolean) =>
        new TestContext(registration) {
          val updatedRegistration: Registration =
            registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", contactAddressIsUk.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (
        registration: Registration,
        contactAddressIsUk: Boolean
      ) =>
        val updatedRegistration = registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

        new TestContext(updatedRegistration) {
          val result: Future[Result]        = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, backRoute.url)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }
}
