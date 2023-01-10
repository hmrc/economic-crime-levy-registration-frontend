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

package uk.gov.hmrc.economiccrimelevyregistration.services

import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

class SubmissionValidationServiceSpec extends SpecBase {
  val service = new SubmissionValidationService()

  "validateRegistrationSubmission" should {
    "return a non-empty chain of errors when unconditional mandatory registration data items are missing" in {
      val registration = Registration.empty("internalId")

      implicit val registrationDataRequest: RegistrationDataRequest[AnyContentAsEmpty.type] =
        RegistrationDataRequest(fakeRequest, "internalId", registration)

      val expectedErrors = Seq(
        DataMissingError("AML supervisor is missing"),
        DataMissingError("Business sector is missing"),
        DataMissingError("First contact name is missing"),
        DataMissingError("First contact role is missing"),
        DataMissingError("First contact email is missing"),
        DataMissingError("First contact number is missing"),
        DataMissingError("Contact address is missing"),
        DataMissingError("Entity type is missing"),
        DataMissingError("Second contact choice is missing")
      )

      val result = service.validateRegistrationSubmission()

      result.leftMap(nec => nec.toNonEmptyList.toList should contain allElementsOf expectedErrors)
    }
  }
}
