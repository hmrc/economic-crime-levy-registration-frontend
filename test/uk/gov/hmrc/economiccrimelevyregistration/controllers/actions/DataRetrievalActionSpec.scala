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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{AuthorisedRequest, RegistrationDataRequest}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase {

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockRegistrationAdditionalInfoService              = mock[RegistrationAdditionalInfoService]
  val mockAppConfig                                      = mock[AppConfig]

  class TestDataRetrievalAction
      extends RegistrationDataRetrievalAction(
        mockEclRegistrationService,
        mockRegistrationAdditionalInfoService,
        mockAppConfig
      ) {
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

        val info = RegistrationAdditionalInfo(
          registration.internalId,
          Some(random[Int]),
          None
        )

        when(mockRegistrationAdditionalInfoService.get(any())(any())).thenReturn(Future.successful(Some(info)))

        val result: Future[Either[Result, RegistrationDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, Some("ECLRefNumber12345")))

        await(result) shouldBe Right(
          RegistrationDataRequest(fakeRequest, internalId, registration, Some(info), Some("ECLRefNumber12345"))
        )
    }

    "transform an AuthorisedRequest into a RegistrationDataRequest when private beta is enabled and the access code matches one that is held in config" in forAll {
      (internalId: String, groupId: String, registration: Registration, privateBetaAccessCode: String) =>
        when(mockAppConfig.privateBetaEnabled).thenReturn(true)
        val updatedRegistration = registration.copy(privateBetaAccessCode = Some(privateBetaAccessCode))
        when(mockEclRegistrationService.getOrCreateRegistration(any())(any()))
          .thenReturn(Future(updatedRegistration))
        when(mockAppConfig.privateBetaAccessCodes).thenReturn(Seq(privateBetaAccessCode))

        val info = RegistrationAdditionalInfo(
          registration.internalId,
          Some(random[Int]),
          None
        )

        when(mockRegistrationAdditionalInfoService.get(any())(any())).thenReturn(Future.successful(Some(info)))

        val result: Future[Either[Result, RegistrationDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, Some("ECLRefNumber12345")))

        await(result) shouldBe Right(
          RegistrationDataRequest(fakeRequest, internalId, updatedRegistration, Some(info), Some("ECLRefNumber12345"))
        )
    }

    "redirect to the private beta access page when private beta is enabled and the access code does not match one that is held in config" in forAll {
      (internalId: String, groupId: String, registration: Registration, privateBetaAccessCode: String) =>
        when(mockAppConfig.privateBetaEnabled).thenReturn(true)
        when(mockEclRegistrationService.getOrCreateRegistration(any())(any()))
          .thenReturn(Future(registration.copy(privateBetaAccessCode = None)))
        when(mockAppConfig.privateBetaAccessCodes).thenReturn(Seq(privateBetaAccessCode))

        val result =
          Future.successful(
            await(dataRetrievalAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, None))).left.value
          )

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PrivateBetaAccessController.onPageLoad(fakeRequest.uri).url)
    }
  }

}
