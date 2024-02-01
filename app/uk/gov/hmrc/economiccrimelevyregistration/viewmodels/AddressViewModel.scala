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

import play.twirl.api.HtmlFormat
import uk.gov.hmrc.economiccrimelevyregistration.models.EclAddress
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText

object AddressViewModel {

  private def eclAddressToSeq(eclAddress: EclAddress): Seq[Option[String]] = Seq(
    eclAddress.organisation,
    eclAddress.addressLine1,
    eclAddress.addressLine2,
    eclAddress.addressLine3,
    eclAddress.addressLine4,
    eclAddress.poBox,
    eclAddress.region,
    eclAddress.postCode
  )

  def html(eclAddress: EclAddress): String = eclAddressToSeq(eclAddress)
    .filter(_.isDefined)
    .map(value => HtmlFormat.escape(value.get))
    .mkString("<br/>")

  def htmlContent(eclAddress: EclAddress): HtmlContent = HtmlContent(html(eclAddress))

  def insetText(eclAddress: EclAddress): InsetText =
    InsetText(
      id = Some("address"),
      content = htmlContent(eclAddress)
    )
}
