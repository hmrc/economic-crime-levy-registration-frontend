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
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.SecondContactEmailFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.SecondContactEmailPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.contacts.SecondContactEmailView

import scala.concurrent.Future

class SecondContactEmailControllerSpec extends SpecBase {

  val view: SecondContactEmailView                 = app.injector.instanceOf[SecondContactEmailView]
  val formProvider: SecondContactEmailFormProvider = new SecondContactEmailFormProvider()
  val form: Form[String]                           = formProvider()

  val pageNavigator: SecondContactEmailPageNavigator = new SecondContactEmailPageNavigator() {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute
  }

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockSessionService: SessionService                 = mock[SessionService]

  class TestContext(registrationData: Registration) {
    val controller = new SecondContactEmailController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      view,
      mockSessionService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(
            contacts = Contacts.empty.copy(secondContactDetails = ContactDetails(name = Some(name), None, None, None)),
            registrationType = Some(Initial)
          )
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          val resultAsString: String = contentAsString(result)

          resultAsString shouldBe view(form, name, NormalMode, None, Some(testEclRegistrationReference))(
            fakeRequest,
            messages
          ).toString

          resultAsString should include("autocomplete=\"email\"")

        }
    }

    "return INTERNAL_SERVER_ERROR when there is no second contact name in the registration data" in forAll {
      (
        registration: Registration
      ) =>
        val updatedRegistration = registration.copy(
          contacts = Contacts.empty
        )

        new TestContext(
          updatedRegistration
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, name: String, email: String) =>
        new TestContext(
          registration.copy(
            contacts = registration.contacts
              .copy(secondContactDetails =
                registration.contacts.secondContactDetails.copy(name = Some(name), emailAddress = Some(email))
              ),
            registrationType = Some(Initial)
          )
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(email),
            name,
            NormalMode,
            None,
            None
          )(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided contact email then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      emailAddress(EmailMaxLength)
    ) { (registration: Registration, email: String) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(emailAddress = Some(email.toLowerCase))
            )
          )

        when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", email)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, name: String) =>
        new TestContext(
          registration.copy(
            contacts = registration.contacts
              .copy(secondContactDetails = registration.contacts.secondContactDetails.copy(name = Some(name))),
            registrationType = Some(Initial)
          )
        ) {
          val result: Future[Result]       =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(
            formWithErrors,
            name,
            NormalMode,
            None,
            None
          )(fakeRequest, messages).toString
        }
    }
  }
}
