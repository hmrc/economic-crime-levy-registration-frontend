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

import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.Languages._
import uk.gov.hmrc.hmrcfrontend.views.config.StandardBetaBanner

final case class AlfEnCyLabels(en: AlfLabels, cy: AlfLabels)

object AlfEnCyLabels {
  def apply(appConfig: AppConfig)(implicit messagesApi: MessagesApi): AlfEnCyLabels = {
    def betaBanner(lang: Lang): String = new StandardBetaBanner()
      .apply(appConfig.feedbackUrl(routes.IsUkAddressController.onPageLoad().url))(messagesApi.preferred(Seq(lang)))
      .content
      .asHtml
      .body

    AlfEnCyLabels(
      en = AlfLabels(
        appLevelLabels = AlfAppLabels(
          navTitle = messagesApi("service.name")(english),
          phaseBannerHtml = betaBanner(english)
        ),
        countryPickerLabels = AlfCountryPickerLabels(
          submitLabel = messagesApi("alf.labels.submit")(english)
        ),
        selectPageLabels = AlfSelectPageLabels(
          title = messagesApi("alf.labels.select.title")(english),
          heading = messagesApi("alf.labels.select.heading")(english),
          submitLabel = messagesApi("alf.labels.submit")(english)
        ),
        lookupPageLabels = AlfLookupPageLabels(
          title = messagesApi("alf.labels.lookup.title")(english),
          heading = messagesApi("alf.labels.lookup.heading")(english),
          postcodeLabel = messagesApi("alf.labels.lookup.postcode")(english),
          submitLabel = messagesApi("alf.labels.submit")(english)
        ),
        editPageLabels = AlfEditPageLabels(
          title = messagesApi("alf.labels.edit.title")(english),
          heading = messagesApi("alf.labels.edit.heading")(english),
          submitLabel = messagesApi("alf.labels.submit")(english)
        )
      ),
      cy = AlfLabels(
        appLevelLabels = AlfAppLabels(
          navTitle = messagesApi("service.name")(welsh),
          phaseBannerHtml = betaBanner(welsh)
        ),
        countryPickerLabels = AlfCountryPickerLabels(
          submitLabel = messagesApi("alf.labels.submit")(welsh)
        ),
        selectPageLabels = AlfSelectPageLabels(
          title = messagesApi("alf.labels.select.title")(welsh),
          heading = messagesApi("alf.labels.select.heading")(welsh),
          submitLabel = messagesApi("alf.labels.submit")(welsh)
        ),
        lookupPageLabels = AlfLookupPageLabels(
          title = messagesApi("alf.labels.lookup.title")(welsh),
          heading = messagesApi("alf.labels.lookup.heading")(welsh),
          postcodeLabel = messagesApi("alf.labels.lookup.postcode")(welsh),
          submitLabel = messagesApi("alf.labels.submit")(welsh)
        ),
        editPageLabels = AlfEditPageLabels(
          title = messagesApi("alf.labels.edit.title")(welsh),
          heading = messagesApi("alf.labels.edit.heading")(welsh),
          submitLabel = messagesApi("alf.labels.submit")(welsh)
        )
      )
    )
  }

  implicit val format: OFormat[AlfEnCyLabels] = Json.format[AlfEnCyLabels]
}

final case class AlfLabels(
  appLevelLabels: AlfAppLabels,
  countryPickerLabels: AlfCountryPickerLabels,
  selectPageLabels: AlfSelectPageLabels,
  lookupPageLabels: AlfLookupPageLabels,
  editPageLabels: AlfEditPageLabels
)

object AlfLabels {
  implicit val format: OFormat[AlfLabels] = Json.format[AlfLabels]
}

final case class AlfAppLabels(navTitle: String, phaseBannerHtml: String)

object AlfAppLabels {
  implicit val format: OFormat[AlfAppLabels] = Json.format[AlfAppLabels]
}

final case class AlfCountryPickerLabels(submitLabel: String)

object AlfCountryPickerLabels {
  implicit val format: OFormat[AlfCountryPickerLabels] = Json.format[AlfCountryPickerLabels]
}

final case class AlfSelectPageLabels(title: String, heading: String, submitLabel: String)

object AlfSelectPageLabels {
  implicit val format: OFormat[AlfSelectPageLabels] = Json.format[AlfSelectPageLabels]
}

final case class AlfLookupPageLabels(title: String, heading: String, postcodeLabel: String, submitLabel: String)

object AlfLookupPageLabels {
  implicit val format: OFormat[AlfLookupPageLabels] = Json.format[AlfLookupPageLabels]
}

final case class AlfEditPageLabels(title: String, heading: String, submitLabel: String)

object AlfEditPageLabels {
  implicit val format: OFormat[AlfEditPageLabels] = Json.format[AlfEditPageLabels]
}
