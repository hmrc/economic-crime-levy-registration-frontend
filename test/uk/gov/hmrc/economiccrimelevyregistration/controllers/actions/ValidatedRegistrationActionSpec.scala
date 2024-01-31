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
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService

import scala.concurrent.Future

class ValidatedRegistrationActionSpec extends SpecBase {

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val validatedRegistrationAction = new ValidatedRegistrationActionImpl(
    mockEclRegistrationService
  )

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "filter" should {
    "return None if the registration data is valid" in forAll { (internalId: String, registration: Registration) =>
      when(mockEclRegistrationService.getRegistrationValidationErrors(any())(any()))
        .thenReturn(EitherT.fromEither[Future](Left(DataRetrievalError.InternalUnexpectedError("", None))))

      val result: Future[Option[Result]] =
        validatedRegistrationAction.filter(
          RegistrationDataRequest(fakeRequest, internalId, registration, None, Some("ECLRefNumber12345"))
        )

      await(result) shouldBe None
    }

    "redirect to the journey recovery page if the registration data is invalid" in forAll {
      (internalId: String, registration: Registration, dataValidationErrors: DataValidationErrors) =>
        when(mockEclRegistrationService.getRegistrationValidationErrors(any())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(dataValidationErrors)))

        val result: Future[Option[Result]] =
          validatedRegistrationAction.filter(
            RegistrationDataRequest(fakeRequest, internalId, registration, None, Some("ECLRefNumber12345"))
          )

        await(result) shouldBe Some(Redirect(routes.NotableErrorController.answersAreInvalid()))
    }
  }

}
