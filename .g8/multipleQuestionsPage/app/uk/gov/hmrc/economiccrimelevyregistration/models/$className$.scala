package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json._

case class $className$($field1Name$: String, $field2Name$: String)

object $className$ {
  implicit val format: OFormat[$className$] = Json.format[$className$]
}
