/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import scala.collection.mutable

trait AdditionalAppConfig {
  val additionalAppConfig: mutable.Map[String, Any] = mutable.Map.empty
}
