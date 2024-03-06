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
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendmentRequestedView

import scala.concurrent.Future

class AmendmentRequestedControllerSpec extends SpecBase {

  val view: AmendmentRequestedView                                 = app.injector.instanceOf[AmendmentRequestedView]
  val mockRegistrationService: EclRegistrationService              = mock[EclRegistrationService]
  val mockAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]

  class TestContext(registrationData: Registration) {
    val controller = new AmendmentRequestedController(
      mcc,
      view,
      fakeAuthorisedActionWithEnrolmentCheck(testInternalId, Some(testEclRegistrationReference)),
      fakeDataRetrievalAction(registrationData),
      mockAdditionalInfoService,
      mockRegistrationService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when there is an email address and contact address" in forAll {
      (
        eclAddress: EclAddress,
        firstContactEmailAddress: String,
        registration: Registration
      ) =>
        val contacts            = registration.contacts.copy(firstContactDetails =
          registration.contacts.firstContactDetails.copy(emailAddress = Some(firstContactEmailAddress))
        )
        val updatedRegistration = registration.copy(contactAddress = Some(eclAddress), contacts = contacts)
        new TestContext(updatedRegistration) {
          when(mockAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest
          )

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            firstContactEmailAddress,
            Some(testEclRegistrationReference),
            Some(eclAddress)
          )(fakeRequest, messages).toString
        }
    }
  }
}
