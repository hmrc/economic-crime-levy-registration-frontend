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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityAddress, IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

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
  contacts: Contacts,
  useRegisteredOfficeAddressAsContactAddress: Option[Boolean],
  contactAddress: Option[EclAddress]
) {

  def grsAddressToEclAddress: Option[EclAddress] = {
    val incorporatedEntityAddress: Option[IncorporatedEntityAddress] =
      (incorporatedEntityJourneyData, soleTraderEntityJourneyData, partnershipEntityJourneyData) match {
        case (Some(d), None, None) => Some(d.companyProfile.unsanitisedCHROAddress)
        case (None, Some(_), None) => None
        case (None, None, Some(d)) => d.companyProfile.map(_.unsanitisedCHROAddress)
        case _                     => None
      }

    incorporatedEntityAddress.map { address =>
      EclAddress(
        addressLine1 = address.address_line_1.map(_.trim),
        addressLine2 = address.address_line_2.map(_.trim),
        townOrCity = address.locality.map(_.trim),
        region = address.region.map(_.trim),
        postCode = address.postal_code.map(_.trim)
      )
    }
  }

}

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
    contacts = Contacts.empty,
    useRegisteredOfficeAddressAsContactAddress = None,
    contactAddress = None
  )
}
