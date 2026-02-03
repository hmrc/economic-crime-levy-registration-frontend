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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.deregister.DeregistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class DeregistrationDataActionSpec extends SpecBase {

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]

  class TestDataActionAction
      extends DeregistrationDataActionImpl(
        mockDeregistrationService
      ) {
    override def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, DeregistrationDataRequest[A]]] =
      super.refine(request)
  }

  val RegistrationDataAction =
    new TestDataActionAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "refine" should {
    "transform an AuthorisedRequest into a DeregistrationDataRequest when data retrieval succeeds" in forAll {
      (internalId: String, groupId: String, deregistration: Deregistration) =>
        when(mockDeregistrationService.getOrCreate(any())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

        val result: Future[Either[Result, DeregistrationDataRequest[AnyContentAsEmpty.type]]] =
          RegistrationDataAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, Some("ECLRefNumber12345")))

        await(result) shouldBe Right(
          DeregistrationDataRequest(fakeRequest, internalId, deregistration, Some("ECLRefNumber12345"))
        )
    }
  }
}
