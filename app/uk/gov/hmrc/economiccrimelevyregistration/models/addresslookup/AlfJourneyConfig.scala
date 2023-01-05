package uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup

import play.api.libs.json.{Json, OFormat}

//TODO Update to include all the relevant fields
final case class AlfJourneyConfig(version: Int = 2, options: AlfOptions, labels: AlfEnCyLabels)

object AlfJourneyConfig {
  implicit val format: OFormat[AlfJourneyConfig] = Json.format[AlfJourneyConfig]
}

final case class AlfOptions(continueUrl: String)

object AlfOptions {
  implicit val format: OFormat[AlfOptions] = Json.format[AlfOptions]
}

final case class AlfEnCyLabels(en: AlfLabels, cy: AlfLabels)

object AlfEnCyLabels {
  implicit val format: OFormat[AlfEnCyLabels] = Json.format[AlfEnCyLabels]
}

final case class AlfLabels(appLevelLabels: AlfAppLabels)

object AlfLabels {
  implicit val format: OFormat[AlfLabels] = Json.format[AlfLabels]
}

final case class AlfAppLabels(navTitle: String, phaseBannerHtml: String)

object AlfAppLabels {
  implicit val format: OFormat[AlfAppLabels] = Json.format[AlfAppLabels]
}
