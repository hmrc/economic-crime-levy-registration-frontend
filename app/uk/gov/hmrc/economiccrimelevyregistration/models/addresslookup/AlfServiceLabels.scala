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

final case class AlfEnCyLabels(en: AlfLabels, cy: AlfLabels)

object AlfEnCyLabels {
  def apply()(implicit messagesApi: MessagesApi): AlfEnCyLabels =
    AlfEnCyLabels(
      en = AlfLabels(
        appLevelLabels = AlfAppLabels(
          navTitle = messagesApi("alf.labels.app.title")(Lang("en")),
          phaseBannerHtml = messagesApi("alf.labels.app.banner")(Lang("en"))
        ),
        countryPickerLabels = AlfCountryPickerLabels(
          submitLabel = messagesApi("alf.labels.submit")(Lang("en"))
        ),
        selectPageLabels = AlfSelectPageLabels(
          title = messagesApi("alf.labels.select.title")(Lang("en")),
          heading = messagesApi("alf.labels.select.heading")(Lang("en")),
          submitLabel = messagesApi("alf.labels.submit")(Lang("en"))
        ),
        lookupPageLabels = AlfLookupPageLabels(
          title = messagesApi("alf.labels.lookup.title")(Lang("en")),
          heading = messagesApi("alf.labels.lookup.heading")(Lang("en")),
          postcodeLabel = messagesApi("alf.labels.lookup.postcode")(Lang("en")),
          submitLabel = messagesApi("alf.labels.submit")(Lang("en"))
        ),
        editPageLabels = AlfEditPageLabels(
          title = messagesApi("alf.labels.edit.title")(Lang("en")),
          heading = messagesApi("alf.labels.edit.heading")(Lang("en")),
          submitLabel = messagesApi("alf.labels.submit")(Lang("en"))
        )
      ),
      cy = AlfLabels(
        appLevelLabels = AlfAppLabels(
          navTitle = messagesApi("alf.labels.app.title")(Lang("cy")),
          phaseBannerHtml = messagesApi("alf.labels.app.banner")(Lang("cy"))
        ),
        countryPickerLabels = AlfCountryPickerLabels(
          submitLabel = messagesApi("alf.labels.submit")(Lang("cy"))
        ),
        selectPageLabels = AlfSelectPageLabels(
          title = messagesApi("alf.labels.select.title")(Lang("cy")),
          heading = messagesApi("alf.labels.select.heading")(Lang("cy")),
          submitLabel = messagesApi("alf.labels.submit")(Lang("cy"))
        ),
        lookupPageLabels = AlfLookupPageLabels(
          title = messagesApi("alf.labels.lookup.title")(Lang("cy")),
          heading = messagesApi("alf.labels.lookup.heading")(Lang("cy")),
          postcodeLabel = messagesApi("alf.labels.lookup.postcode")(Lang("cy")),
          submitLabel = messagesApi("alf.labels.submit")(Lang("cy"))
        ),
        editPageLabels = AlfEditPageLabels(
          title = messagesApi("alf.labels.edit.title")(Lang("cy")),
          heading = messagesApi("alf.labels.edit.heading")(Lang("cy")),
          submitLabel = messagesApi("alf.labels.submit")(Lang("cy"))
        )
      )
    )

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
