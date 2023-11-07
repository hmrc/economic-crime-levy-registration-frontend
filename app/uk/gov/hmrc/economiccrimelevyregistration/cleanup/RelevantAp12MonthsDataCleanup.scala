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

class RelevantAp12MonthsDataCleanup @Inject() () extends DataCleanup {
  def cleanup(registration: Registration, change: Boolean): Registration =
    registration.relevantAp12Months match {
      case Some(true) => registration.copy(relevantApLength = None, revenueMeetsThreshold = None)
      case _          => registration.copy(revenueMeetsThreshold = None)
    }

}
