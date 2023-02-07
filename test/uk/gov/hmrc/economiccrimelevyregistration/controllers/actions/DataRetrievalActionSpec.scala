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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{AuthorisedRequest, RegistrationDataRequest}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase {

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  class TestDataRetrievalAction extends RegistrationDataRetrievalAction(mockEclRegistrationService) {
    override def transform[A](request: AuthorisedRequest[A]): Future[RegistrationDataRequest[A]] =
      super.transform(request)
  }

  val dataRetrievalAction =
    new TestDataRetrievalAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "transform" should {
    "transform an AuthorisedRequest into a RegistrationDataRequest" in forAll {
      (internalId: String, groupId: String, registration: Registration) =>
        when(mockEclRegistrationService.getOrCreateRegistration(any())(any())).thenReturn(Future(registration))

        val result: Future[RegistrationDataRequest[AnyContentAsEmpty.type]] =
          dataRetrievalAction.transform(AuthorisedRequest(fakeRequest, internalId, groupId, None))

        await(result) shouldBe RegistrationDataRequest(fakeRequest, internalId, registration)
    }
  }

}
