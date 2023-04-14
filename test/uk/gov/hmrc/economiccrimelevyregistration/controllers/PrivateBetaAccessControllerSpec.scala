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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.PrivateBetaAccessFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.PrivateBetaAccessView

import scala.concurrent.Future

class PrivateBetaAccessControllerSpec extends SpecBase {

  val view: PrivateBetaAccessView                 = app.injector.instanceOf[PrivateBetaAccessView]
  val mockAppConfig: AppConfig                    = mock[AppConfig]
  val formProvider: PrivateBetaAccessFormProvider = new PrivateBetaAccessFormProvider(mockAppConfig)
  val form: Form[String]                          = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val mockEclRegistrationService: EclRegistrationService     = mock[EclRegistrationService]

  val controller = new PrivateBetaAccessController(
    mcc,
    fakeAuthorisedActionWithEnrolmentCheck(testInternalId),
    mockEclRegistrationService,
    mockEclRegistrationConnector,
    formProvider,
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll { continueUrl: String =>
      val result: Future[Result] = controller.onPageLoad(continueUrl)(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe view(form, continueUrl)(fakeRequest, messages).toString
    }
  }

  "onSubmit" should {
    "save the access code if it matches one held in config and redirect to the continue URL" in forAll(
      Arbitrary.arbitrary[Registration],
      nonBlankString
    ) { (registration: Registration, accessCode: String) =>
      when(mockAppConfig.privateBetaAccessCodes).thenReturn(Seq(accessCode))

      when(mockEclRegistrationService.getOrCreateRegistration(any())(any())).thenReturn(Future.successful(registration))

      val updatedRegistration = registration.copy(privateBetaAccessCode = Some(accessCode))

      when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
        .thenReturn(Future.successful(updatedRegistration))

      val result: Future[Result] =
        controller.onSubmit(fakeRequest.uri)(fakeRequest.withFormUrlEncodedBody(("value", accessCode)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(fakeRequest.uri)
    }

    "return a Bad Request with form errors when invalid data is submitted" in {
      val result: Future[Result]       =
        controller.onSubmit(fakeRequest.uri)(fakeRequest.withFormUrlEncodedBody(("value", "")))
      val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

      status(result) shouldBe BAD_REQUEST

      contentAsString(result) shouldBe view(formWithErrors, fakeRequest.uri)(fakeRequest, messages).toString
    }
  }
}
