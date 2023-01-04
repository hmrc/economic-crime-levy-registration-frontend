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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels

import uk.gov.hmrc.economiccrimelevyregistration.models.EclAddress
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText

object InsetTextAddress {

  def apply(eclAddress: EclAddress): InsetText = {
    val optLineWithBreak: Option[String] => String = s => s.fold("")(l => s"$l <br>")

    val html = s"""
      |<p class="govuk-body">
      |${optLineWithBreak(eclAddress.addressLine1)}
      |${optLineWithBreak(eclAddress.addressLine2)}
      |${optLineWithBreak(eclAddress.addressLine3)}
      |${optLineWithBreak(eclAddress.townOrCity)}
      |${optLineWithBreak(eclAddress.region)}
      |${optLineWithBreak(eclAddress.postCode)}
      |</p>
      |""".stripMargin

    InsetText(
      id = Some("address"),
      content = HtmlContent(html)
    )
  }
}
