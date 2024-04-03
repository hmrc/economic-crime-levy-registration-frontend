/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.models

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

class RegistrationSpec extends SpecBase {

  "isVoid" should {
    "return false if both carriedOutAmlRegulatedActivityInCurrentFy and revenueMeetsThreshold are not false" in forAll {
      (
        registration: Registration,
        carriedOutAmlRegulatedActivityInCurrentFy: Boolean,
        revenueMeetsThreshold: Boolean
      ) =>
        def setValue(value: Boolean) = value match {
          case false => None
          case true  => Some(value)
        }

        registration
          .copy(
            carriedOutAmlRegulatedActivityInCurrentFy = setValue(carriedOutAmlRegulatedActivityInCurrentFy),
            revenueMeetsThreshold = setValue(revenueMeetsThreshold)
          )
          .isVoid shouldBe false
    }

    "return true if either carriedOutAmlRegulatedActivityInCurrentFy or revenueMeetsThreshold is false" in forAll {
      (
        registration: Registration,
        carriedOutAmlRegulatedActivityInCurrentFy: Boolean,
        revenueMeetsThreshold: Boolean
      ) =>
        if (!carriedOutAmlRegulatedActivityInCurrentFy || !revenueMeetsThreshold) {

          registration
            .copy(
              carriedOutAmlRegulatedActivityInCurrentFy = Some(carriedOutAmlRegulatedActivityInCurrentFy),
              revenueMeetsThreshold = Some(revenueMeetsThreshold)
            )
            .isVoid shouldBe true
        }
    }
  }
}
