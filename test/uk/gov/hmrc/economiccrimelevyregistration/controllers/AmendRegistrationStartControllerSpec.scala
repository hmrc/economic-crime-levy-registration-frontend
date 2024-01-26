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
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.RegistrationError
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendRegistrationStartView

import scala.concurrent.Future

class AmendRegistrationStartControllerSpec extends SpecBase {

  val view: AmendRegistrationStartView                                         = app.injector.instanceOf[AmendRegistrationStartView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockRegistrationService: EclRegistrationService                          = mock[EclRegistrationService]

  val controller = new AmendRegistrationStartController(
    mcc,
    mockRegistrationAdditionalInfoService,
    fakeAuthorisedActionWithEnrolmentCheck(testInternalId),
    view,
    mockRegistrationService
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll { registration: Registration =>
      when(
        mockRegistrationAdditionalInfoService.createOrUpdate(
          any()
        )(any(), any())
      ).thenReturn(
        EitherT.fromEither[Future](Right())
      )

      when(mockRegistrationService.getOrCreateRegistration(any()))
        .thenReturn(EitherT.fromEither(Right(registration)))

      val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe view("eclReferenceValue")(fakeRequest, messages).toString
    }

    "return Internal server error and the correct view" in forAll { registration: Registration =>
      when(mockRegistrationService.getOrCreateRegistration(any()))
        .thenReturn(EitherT.fromEither[Future](Right(registration)))

      when(
        mockRegistrationAdditionalInfoService.createOrUpdate(
          any()
        )(any(), any())
      ).thenReturn(
        EitherT.fromEither[Future](Left(RegistrationError.InternalUnexpectedError("", None)))
      )

      val result: Future[Result] = controller.onPageLoad("eclReferenceValue")(fakeRequest)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid())
    }
  }

}
