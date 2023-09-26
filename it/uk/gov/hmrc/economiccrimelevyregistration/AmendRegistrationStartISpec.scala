/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationAdditionalInfo

class AmendRegistrationStartISpec extends ISpecBase {

  s"GET ${routes.AmendRegistrationStartController.onPageLoad(testInternalId).url}" should {
    "respond with 200 status and the start HTML view" in {

      stubAuthorisedWithEclEnrolment()

      stubUpsertRegistrationAdditionalInfo(RegistrationAdditionalInfo(testInternalId, None, None))

      val result = callRoute(FakeRequest(routes.AmendRegistrationStartController.onPageLoad(testInternalId)))

      status(result) shouldBe OK
      html(result)     should include("Register for the Economic Crime Levy")
    }
  }

}
