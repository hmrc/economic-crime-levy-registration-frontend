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

import cats.implicits._
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{DataMissingError, SubmissionValidationService}

import scala.concurrent.Future

class ValidationSubmissionActionSpec extends SpecBase {

  val mockSubmissionValidationService: SubmissionValidationService = mock[SubmissionValidationService]

  val validatedSubmissionAction = new ValidatedSubmissionActionImpl(mockSubmissionValidationService)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "filter" should {
    "return None if the registration data is valid" in forAll { (internalId: String, registration: Registration) =>
      when(mockSubmissionValidationService.validateRegistrationSubmission()(any()))
        .thenReturn(registration.validNec)

      val result: Future[Option[Result]] =
        validatedSubmissionAction.filter(RegistrationDataRequest(fakeRequest, internalId, registration))

      await(result) shouldBe None
    }

    "return to the start page if the registration data is invalid" in forAll {
      (internalId: String, registration: Registration) =>
        when(mockSubmissionValidationService.validateRegistrationSubmission()(any()))
          .thenReturn(DataMissingError("Some data is missing").invalidNec)

        val result: Future[Option[Result]] =
          validatedSubmissionAction.filter(RegistrationDataRequest(fakeRequest, internalId, registration))

        await(result) shouldBe Some(Redirect(routes.StartController.onPageLoad()))
    }
  }

}
