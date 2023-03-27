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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions

import org.mockito.ArgumentMatchers.any
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{AuthorisedRequest, RegistrationDataRequest}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase {

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockAppConfig                                      = mock[AppConfig]

  class TestDataRetrievalAction extends RegistrationDataRetrievalAction(mockEclRegistrationService, mockAppConfig) {
    override def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, RegistrationDataRequest[A]]] =
      super.refine(request)
  }

  val dataRetrievalAction =
    new TestDataRetrievalAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "refine" should {
    "transform an AuthorisedRequest into a RegistrationDataRequest when private beta is disabled" in forAll {
      (internalId: String, groupId: String, registration: Registration) =>
        when(mockAppConfig.privateBetaEnabled).thenReturn(false)
        when(mockEclRegistrationService.getOrCreateRegistration(any())(any())).thenReturn(Future(registration))

        val result: Future[Either[Result, RegistrationDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, None))

        await(result) shouldBe Right(RegistrationDataRequest(fakeRequest, internalId, registration))
    }

    "transform an AuthorisedRequest into a RegistrationDataRequest when private beta is enabled and the access code matches what is held in config" in forAll {
      (internalId: String, groupId: String, registration: Registration, privateBetaAccessCode: String) =>
        when(mockAppConfig.privateBetaEnabled).thenReturn(true)
        val updatedRegistration = registration.copy(privateBetaAccessCode = Some(privateBetaAccessCode))
        when(mockEclRegistrationService.getOrCreateRegistration(any())(any()))
          .thenReturn(Future(updatedRegistration))
        when(mockAppConfig.privateBetaAccessCode).thenReturn(privateBetaAccessCode)

        val result: Future[Either[Result, RegistrationDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, None))

        await(result) shouldBe Right(RegistrationDataRequest(fakeRequest, internalId, updatedRegistration))
    }

    "redirect to the private beta access page when private beta is enabled and the access code does not match what is held in config" in forAll {
      (internalId: String, groupId: String, registration: Registration, privateBetaAccessCode: String) =>
        when(mockAppConfig.privateBetaEnabled).thenReturn(true)
        when(mockEclRegistrationService.getOrCreateRegistration(any())(any()))
          .thenReturn(Future(registration.copy(privateBetaAccessCode = None)))
        when(mockAppConfig.privateBetaAccessCode).thenReturn(privateBetaAccessCode)

        val result =
          Future.successful(
            await(dataRetrievalAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, None))).left.value
          )

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PrivateBetaAccessController.onPageLoad(fakeRequest.uri).url)
    }
  }

}
