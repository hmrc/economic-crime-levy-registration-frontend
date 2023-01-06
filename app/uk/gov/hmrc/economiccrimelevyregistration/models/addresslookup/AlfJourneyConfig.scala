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

package uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup

import play.api.libs.json.{Json, OFormat}

final case class AlfJourneyConfig(version: Int = 2, options: AlfOptions, labels: AlfEnCyLabels)

object AlfJourneyConfig {
  implicit val format: OFormat[AlfJourneyConfig] = Json.format[AlfJourneyConfig]
}

final case class AlfOptions(
  continueUrl: String,
  homeNavHref: String,
  signOutHref: String,
  accessibilityFooterUrl: String,
  deskProServiceName: String,
  showPhaseBanner: Boolean = true,
  includeHMRCBranding: Boolean = false,
  pageHeadingStyle: String = "govuk-heading-l",
  ukMode: Boolean
)

object AlfOptions {
  implicit val format: OFormat[AlfOptions] = Json.format[AlfOptions]
}
