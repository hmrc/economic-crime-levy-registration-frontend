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
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

import scala.concurrent.Future

class ValidatedRegistrationActionSpec extends SpecBase {

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val validatedRegistrationAction = new ValidatedRegistrationActionImpl(mockEclRegistrationConnector)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "filter" should {
    "return None if the registration data is valid" in forAll { (internalId: String, registration: Registration) =>
      when(mockEclRegistrationConnector.getRegistrationValidationErrors(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Option[Result]] =
        validatedRegistrationAction.filter(RegistrationDataRequest(fakeRequest, internalId, registration))

      await(result) shouldBe None
    }

    "return to the start page if the registration data is invalid" in forAll {
      (internalId: String, registration: Registration, dataValidationErrors: DataValidationErrors) =>
        when(mockEclRegistrationConnector.getRegistrationValidationErrors(any())(any()))
          .thenReturn(Future.successful(Some(dataValidationErrors)))

        val result: Future[Option[Result]] =
          validatedRegistrationAction.filter(RegistrationDataRequest(fakeRequest, internalId, registration))

        await(result) shouldBe Some(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

}
