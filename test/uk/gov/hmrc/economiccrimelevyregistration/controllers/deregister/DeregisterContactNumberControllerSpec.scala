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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataRetrievalAction
import uk.gov.hmrc.economiccrimelevyregistration.forms.deregister.DeregisterContactNumberFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.TelephoneNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.{arbDeregistration, arbMode}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterContactNumberView

import scala.concurrent.Future

class DeregisterContactNumberControllerSpec extends SpecBase {

  val view: DeregisterContactNumberView                 = app.injector.instanceOf[DeregisterContactNumberView]
  val formProvider: DeregisterContactNumberFormProvider = new DeregisterContactNumberFormProvider()

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]

  class TestContext(internalId: String) {
    val controller = new DeregisterContactNumberController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(internalId),
      new DeregistrationDataRetrievalAction(mockDeregistrationService),
      mockDeregistrationService,
      formProvider,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { (deregistration: Deregistration, name: String, mode: Mode) =>
      val updatedDeregistration: Deregistration =
        deregistration.copy(contactDetails = deregistration.contactDetails.copy(name = Some(name)))
      new TestContext(updatedDeregistration.internalId) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(
            EitherT.fromEither[Future](
              Right(updatedDeregistration)
            )
          )

        val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

        val form = updatedDeregistration.contactDetails.telephoneNumber match {
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
  }

  "onSubmit" should {
    "go to check your answers view" in forAll(
      Arbitrary.arbitrary[Deregistration],
      telephoneNumber(TelephoneNumberMaxLength)
    ) { (deregistration: Deregistration, number: String) =>
      new TestContext(deregistration.internalId) {
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
  }
}
