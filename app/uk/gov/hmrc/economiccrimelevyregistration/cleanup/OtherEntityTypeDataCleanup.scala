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

package uk.gov.hmrc.economiccrimelevyregistration.cleanup

import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

import javax.inject.Inject

class OtherEntityTypeDataCleanup @Inject() () extends DataCleanup {
  def cleanup(registration: Registration): Registration = {
    val otherEntityJourneyData = registration.otherEntityJourneyData.copy(
      businessName = None,
      charityRegistrationNumber = None,
      companyRegistrationNumber = None,
      utrType = None,
      ctUtr = None,
      saUtr = None,
      postcode = None,
      isCtUtrPresent = None
    )
    registration.copy(
      optOtherEntityJourneyData = Some(otherEntityJourneyData)
    )
  }
}
