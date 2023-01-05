package uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup

import play.api.libs.json.{Json, OFormat}

final case class AlfAddressData(id: Option[String], address: AlfAddress)

object AlfAddressData {
  implicit val format: OFormat[AlfAddressData] = Json.format[AlfAddressData]
}

final case class AlfAddress(lines: Seq[String], postcode: Option[String], country: AlfCountry)

object AlfAddress {
  implicit val format: OFormat[AlfAddress] = Json.format[AlfAddress]
}

final case class AlfCountry(code: String, name: String)

object AlfCountry {
  implicit val format: OFormat[AlfCountry] = Json.format[AlfCountry]
}
