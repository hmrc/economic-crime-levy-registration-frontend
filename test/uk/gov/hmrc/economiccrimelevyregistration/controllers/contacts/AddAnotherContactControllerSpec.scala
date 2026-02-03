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

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.AddAnotherContactDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.AddAnotherContactFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.AddAnotherContactPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.contacts.AddAnotherContactView
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class AddAnotherContactControllerSpec extends SpecBase {

  val view: AddAnotherContactView                 = app.injector.instanceOf[AddAnotherContactView]
  val formProvider: AddAnotherContactFormProvider = new AddAnotherContactFormProvider()
  val form: Form[Boolean]                         = formProvider()

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockDataCleanup: AddAnotherContactDataCleanup      = mock[AddAnotherContactDataCleanup]

  val pageNavigator: AddAnotherContactPageNavigator = new AddAnotherContactPageNavigator {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute

    override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new AddAnotherContactController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      mockDataCleanup,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration) =>
        new TestContext(
          registration
            .copy(contacts = registration.contacts.copy(secondContact = None), registrationType = Some(Initial))
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, NormalMode, None, None)(
            fakeRequest,
            messages
          ).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, secondContact: Boolean) =>
        new TestContext(
          registration.copy(
            contacts = registration.contacts.copy(secondContact = Some(secondContact)),
            registrationType = Some(Initial)
          )
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(
            form.fill(secondContact),
            NormalMode,
            None,
            None
          )(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page when answer is true" in forAll {
      (registration: Registration) =>
        new TestContext(registration) {

          val contacts: Contacts                = registration.contacts.copy(secondContact = Some(true))
          val updatedRegistration: Registration = registration.copy(contacts = contacts)

          when(mockDataCleanup.cleanup(any())).thenReturn(updatedRegistration)

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "true")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "save the selected answer then redirect to the next page and the answer is no and there was an existing second contact" in forAll {
      (registration: Registration) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts.copy(
              secondContact = Some(true),
              secondContactDetails = ContactDetails(
                Some(alphaNumericString),
                Some(alphaNumericString),
                Some(alphaNumericString),
                Some(alphaNumericString)
              )
            )
          )
        ) {
          val updatedRegistration: Registration = registration.copy(contacts =
            registration.contacts.copy(
              secondContact = Some(false),
              secondContactDetails = ContactDetails.empty
            )
          )

          when(mockDataCleanup.cleanup(any())).thenReturn(updatedRegistration)

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "false")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "save the selected answer then redirect to the next page and the answer is no and there was no existing second contact" in forAll {
      (registration: Registration) =>
        new TestContext(
          registration.copy(contacts =
            registration.contacts.copy(
              secondContact = Some(false),
              secondContactDetails = ContactDetails.empty
            )
          )
        ) {
          val updatedRegistration: Registration = registration.copy(contacts =
            registration.contacts.copy(
              secondContact = Some(false),
              secondContactDetails = ContactDetails.empty
            )
          )

          when(mockDataCleanup.cleanup(any())).thenReturn(updatedRegistration)

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "false")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { (registration: Registration) =>
      val updatedRegistration = registration.copy(registrationType = Some(Initial))
      new TestContext(updatedRegistration) {
        val result: Future[Result]        = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
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
