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

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, RegistrationType, SessionData, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionService
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class StoreUrlActionSpec extends SpecBase {

  val mockSessionService: SessionService = mock[SessionService]

  class TestStoreUrlAction extends StoreUrlAction(mockSessionService) {
    override def refine[A](
      request: RegistrationDataRequest[A]
    ): Future[Either[Result, RegistrationDataRequest[A]]] =
      super.refine(request)
  }

  val storeUrlAction = new TestStoreUrlAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "refine" should {
    "store given url if Registration type is Initial" in forAll { (registration: Registration, url: String) =>
      val sessionData = SessionData(registration.internalId, Map(SessionKeys.UrlToReturnTo -> url))
      when(mockSessionService.upsert(ArgumentMatchers.eq(sessionData))(any()))
        .thenReturn(EitherT.fromEither[Future](Right(())))

      val request = FakeRequest(GET, url)

      await(
        storeUrlAction.refine(
          RegistrationDataRequest(
            request,
            registration.internalId,
            registration.copy(registrationType = Some(Initial)),
            None,
            None
          )
        )
      )

      verify(mockSessionService, times(1)).upsert(any())(any())

      reset(mockSessionService)
    }

    "does not store given url if Registration type is not Initial" in forAll(
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[RegistrationType].retryUntil(_ != Initial)
    ) { (registration: Registration, url: String, registrationType: RegistrationType) =>
      when(mockSessionService.upsert(any())(any()))
        .thenReturn(EitherT.fromEither[Future](Right(())))

      val request = FakeRequest(GET, url)

      await(
        storeUrlAction.refine(
          RegistrationDataRequest(
            request,
            registration.internalId,
            registration.copy(registrationType = Some(registrationType)),
            None,
            None
          )
        )
      )

      verify(mockSessionService, times(0)).upsert(any())(any())

      reset(mockSessionService)
    }
  }

}
