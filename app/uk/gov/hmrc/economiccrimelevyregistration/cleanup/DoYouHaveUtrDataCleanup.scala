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

package uk.gov.hmrc.economiccrimelevyregistration.cleanup

import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

object DoYouHaveUtrDataCleanup extends DataCleanup {
  def cleanup(registration: Registration): Registration = {
    val hasUtr = registration.otherEntityJourneyData.isCtUtrPresent.contains(true)

    val otherEntityJourneyData = {
      if (registration.isUnincorporatedAssociation && !hasUtr) {
        registration.otherEntityJourneyData.copy(
          utrType = None,
          ctUtr = None,
          saUtr = None,
          postcode = None
        )
      } else {
        registration.otherEntityJourneyData.copy(
          ctUtr = hasUtr match {
            case false => None
            case true  => registration.otherEntityJourneyData.ctUtr
          },
          utrType = registration.otherEntityJourneyData.utrType
        )
      }
    }

    registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
  }

}
