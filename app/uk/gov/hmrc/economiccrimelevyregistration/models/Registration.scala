/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

import java.time.LocalDate

final case class Registration(
  internalId: String,
  entityType: Option[EntityType],
  meetsRevenueThreshold: Option[Boolean],
  amlSupervisor: Option[AmlSupervisor],
  incorporatedEntityJourneyData: Option[IncorporatedEntityJourneyData],
  soleTraderEntityJourneyData: Option[SoleTraderEntityJourneyData],
  partnershipEntityJourneyData: Option[PartnershipEntityJourneyData],
  startedAmlRegulatedActivityInCurrentFy: Option[Boolean],
  amlRegulatedActivityStartDate: Option[LocalDate],
  businessSector: Option[BusinessSector],
  contacts: Contacts
)

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]

  def empty(internalId: String): Registration = Registration(
    internalId = internalId,
    entityType = None,
    meetsRevenueThreshold = None,
    amlSupervisor = None,
    incorporatedEntityJourneyData = None,
    soleTraderEntityJourneyData = None,
    partnershipEntityJourneyData = None,
    startedAmlRegulatedActivityInCurrentFy = None,
    amlRegulatedActivityStartDate = None,
    businessSector = None,
    contacts = Contacts.empty
  )
}
