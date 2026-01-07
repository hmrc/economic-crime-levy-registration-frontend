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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.IsUkAddressFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{AddressLookupContinueError, DataRetrievalError}
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.services.{AddressLookupService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.IsUkAddressView
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class IsUkAddressControllerSpec extends SpecBase {

  val view: IsUkAddressView                   = app.injector.instanceOf[IsUkAddressView]
  val formProvider: IsUkAddressFormProvider   = new IsUkAddressFormProvider()
  val form: Form[Boolean]                     = formProvider()
  val mockAddressLookup: AddressLookupService = mock[AddressLookupService]

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  class TestContext(registrationData: Registration) {
    val controller = new IsUkAddressController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      view,
      mockAddressLookup
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (
        registration: Registration
      ) =>
        val updatedRegistration = registration.copy(contactAddressIsUk = None, registrationType = Some(Initial))

        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, NormalMode, None, None)(
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
        val updatedRegistration =
          registration.copy(contactAddressIsUk = Some(contactAddressIsUk), registrationType = Some(Initial))

        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(
            form.fill(contactAddressIsUk),
            NormalMode,
            None,
            None
          )(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, contactAddressIsUk: Boolean, journeyAddress: String) =>
        new TestContext(registration) {
          val updatedRegistration: Registration =
            registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))
          when(
            mockAddressLookup.initJourney(ArgumentMatchers.eq(contactAddressIsUk), ArgumentMatchers.eq(NormalMode))(
              any()
            )
          )
            .thenReturn(EitherT[Future, AddressLookupContinueError, String](Future.successful(Right(journeyAddress))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", contactAddressIsUk.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(journeyAddress)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (
        registration: Registration,
        contactAddressIsUk: Boolean
      ) =>
        val updatedRegistration =
          registration.copy(contactAddressIsUk = Some(contactAddressIsUk), registrationType = Some(Initial))

        new TestContext(updatedRegistration) {
          val result: Future[Result]        =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, NormalMode, None, None)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }
}
