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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, anyString}
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.deregister.DeregisterContactNumberFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.TelephoneNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.{arbDeregistration, arbMode}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterContactNumberView

import scala.concurrent.Future

class DeregisterContactNumberControllerSpec extends SpecBase {

  val view: DeregisterContactNumberView                 = app.injector.instanceOf[DeregisterContactNumberView]
  val formProvider: DeregisterContactNumberFormProvider = new DeregisterContactNumberFormProvider()
  val form: Form[String]                                = formProvider()

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]

  class TestContext(deregistration: Deregistration) {
    val controller = new DeregisterContactNumberController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(deregistration.internalId),
      fakeDeregistrationDataAction(deregistration),
      mockDeregistrationService,
      formProvider,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { (deregistration: Deregistration, name: String, mode: Mode) =>
      val updatedDeregistration: Deregistration =
        deregistration.copy(contactDetails = deregistration.contactDetails.copy(name = Some(name)))
      new TestContext(updatedDeregistration) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(
            EitherT.fromEither[Future](
              Right(updatedDeregistration)
            )
          )

        val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

        val form: Form[String] = updatedDeregistration.contactDetails.telephoneNumber match {
          case Some(number) => formProvider().fill(number)
          case None         => formProvider()
        }

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          form,
          name,
          mode,
          deregistration.registrationType
        )(fakeRequest, messages).toString
      }
    }

    "return an Internal Server error when there is no contact name present" in forAll {
      (deregistration: Deregistration, mode: Mode) =>
        val updatedDeregistration: Deregistration =
          deregistration.copy(contactDetails = ContactDetails.empty)
        new TestContext(updatedDeregistration) {

          when(mockDeregistrationService.getOrCreate(anyString())(any()))
            .thenReturn(
              EitherT.fromEither[Future](
                Right(updatedDeregistration)
              )
            )

          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR

        }
    }

  }

  "onSubmit" should {
    "go to check your answers view" in forAll(
      Arbitrary.arbitrary[Deregistration],
      telephoneNumber(TelephoneNumberMaxLength)
    ) { (deregistration: Deregistration, number: String) =>
      new TestContext(deregistration) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

        when(mockDeregistrationService.upsert(any())(any()))
          .thenReturn(
            EitherT.fromEither[Future](
              Right(
                deregistration.copy(contactDetails = deregistration.contactDetails.copy(telephoneNumber = Some(number)))
              )
            )
          )

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> number))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DeregisterCheckYourAnswersController.onPageLoad().url)

        verify(mockDeregistrationService, times(1)).upsert(any())(any())
        reset(mockDeregistrationService)
      }
    }

    "return an error when the upsert fails" in forAll(
      Arbitrary.arbitrary[Deregistration],
      telephoneNumber(TelephoneNumberMaxLength)
    ) { (deregistration: Deregistration, number: String) =>
      new TestContext(deregistration) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))
        when(mockDeregistrationService.upsert(any())(any()))
          .thenReturn(
            EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("Unable to upsert", None)))
          )

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> number))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "return a BadRequest form with errors when there is a contact name present but an answer has not been provided" in forAll {
    (deregistration: Deregistration, mode: Mode, name: String) =>
      val updatedDeregistration: Deregistration =
        deregistration.copy(contactDetails = deregistration.contactDetails.copy(name = Some(name)))

      new TestContext(updatedDeregistration) {

        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(updatedDeregistration)))

        val result: Future[Result]       =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody("value" -> ""))

        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result)          shouldBe BAD_REQUEST
        contentAsString(result) shouldBe view(
          formWithErrors,
          updatedDeregistration.contactDetails.name.get,
          mode,
          updatedDeregistration.registrationType
        )(fakeRequest, messages).toString
      }
      reset(mockDeregistrationService)
  }

}
