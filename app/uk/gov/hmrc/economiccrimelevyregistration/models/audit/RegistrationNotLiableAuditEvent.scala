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

package uk.gov.hmrc.economiccrimelevyregistration.models.audit

import play.api.libs.json._

sealed trait NotLiableReason

object NotLiableReason {
  case object DidNotCarryOutAmlRegulatedActivity extends NotLiableReason
  case object SupervisedByGamblingCommission extends NotLiableReason
  case object SupervisedByFinancialConductAuthority extends NotLiableReason
  case class RevenueDoesNotMeetThreshold(
    relevantAp12Months: Option[Boolean],
    relevantApLength: Option[Int],
    relevantApRevenue: Long,
    revenueMeetsThreshold: Boolean
  ) extends NotLiableReason

  object RevenueDoesNotMeetThreshold {
    implicit val writes: OWrites[RevenueDoesNotMeetThreshold] = Json.writes[RevenueDoesNotMeetThreshold]
  }

  implicit val writes: Writes[NotLiableReason] = {
    case r @ RevenueDoesNotMeetThreshold(_, _, _, _) => Json.toJson(r)
    case o                                           => JsString(o.toString)
  }
}

case class RegistrationNotLiableAuditEvent(internalId: String, notLiableReason: NotLiableReason) extends AuditEvent {
  override val auditType: String   = "RegistrationNotLiable"
  override val detailJson: JsValue = Json.toJson(this)
}

object RegistrationNotLiableAuditEvent {
  implicit val writes: OWrites[RegistrationNotLiableAuditEvent] = Json.writes[RegistrationNotLiableAuditEvent]
}
