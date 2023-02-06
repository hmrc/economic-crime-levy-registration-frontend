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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityAddress, IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

import java.util.Date

final case class Registration(
  internalId: String,
  carriedOutAmlRegulatedActivityInCurrentFy: Option[Boolean],
  entityType: Option[EntityType],
  amlSupervisor: Option[AmlSupervisor],
  relevantAp12Months: Option[Boolean],
  relevantApLength: Option[Int],
  relevantApRevenue: Option[Long],
  revenueMeetsThreshold: Option[Boolean],
  incorporatedEntityJourneyData: Option[IncorporatedEntityJourneyData],
  soleTraderEntityJourneyData: Option[SoleTraderEntityJourneyData],
  partnershipEntityJourneyData: Option[PartnershipEntityJourneyData],
  businessSector: Option[BusinessSector],
  contacts: Contacts,
  useRegisteredOfficeAddressAsContactAddress: Option[Boolean],
  contactAddress: Option[EclAddress],
  contactAddressIsUk: Option[Boolean]
) {

  def grsAddressToEclAddress: Option[EclAddress] = {
    val incorporatedEntityAddress: Option[IncorporatedEntityAddress] =
      (incorporatedEntityJourneyData, soleTraderEntityJourneyData, partnershipEntityJourneyData) match {
        case (Some(d), None, None) => Some(d.companyProfile.unsanitisedCHROAddress)
        case (None, Some(_), None) => None
        case (None, None, Some(d)) => d.companyProfile.map(_.unsanitisedCHROAddress)
        case _                     => None
      }

    incorporatedEntityAddress.flatMap { address =>
      address.address_line_1.map { addressLine1 =>
        EclAddress(
          organisation = None,
          addressLine1 = address.premises.fold(Some(addressLine1))(p => Some(s"$p $addressLine1")),
          addressLine2 = address.address_line_2.map(_.trim),
          addressLine3 = address.locality.map(_.trim),
          addressLine4 = None,
          region = address.region.map(_.trim),
          postCode = address.postal_code.map(_.trim),
          poBox = address.po_box.map(_.trim),
          countryCode = "GB"
        )
      }
    }
  }

  def entityName: Option[String] =
    (incorporatedEntityJourneyData, partnershipEntityJourneyData, soleTraderEntityJourneyData) match {
      case (Some(i), _, _) => Some(i.companyProfile.companyName)
      case (_, Some(p), _) => p.companyProfile.map(_.companyName)
      case (_, _, Some(s)) => Some(s"${s.fullName.firstName} ${s.fullName.lastName}")
      case _               => None
    }

  def companyNumber: Option[String] =
    (incorporatedEntityJourneyData, partnershipEntityJourneyData) match {
      case (Some(i), _) => Some(i.companyProfile.companyNumber)
      case (_, Some(p)) => p.companyProfile.map(_.companyNumber)
      case _            => None
    }

  def ctUtr: Option[String] = incorporatedEntityJourneyData.map(_.ctutr)

  def saUtr: Option[String] = (partnershipEntityJourneyData, soleTraderEntityJourneyData) match {
    case (Some(p), _) => p.sautr
    case (_, Some(s)) => s.sautr
    case _            => None
  }

  def nino: Option[String] = soleTraderEntityJourneyData.flatMap(_.nino)

  def dateOfBirth: Option[Date] = soleTraderEntityJourneyData.map(_.dateOfBirth)

}

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]

  def empty(internalId: String): Registration = Registration(
    internalId = internalId,
    entityType = None,
    amlSupervisor = None,
    relevantAp12Months = None,
    relevantApLength = None,
    relevantApRevenue = None,
    revenueMeetsThreshold = None,
    incorporatedEntityJourneyData = None,
    soleTraderEntityJourneyData = None,
    partnershipEntityJourneyData = None,
    carriedOutAmlRegulatedActivityInCurrentFy = None,
    businessSector = None,
    contacts = Contacts.empty,
    useRegisteredOfficeAddressAsContactAddress = None,
    contactAddress = None,
    contactAddressIsUk = None
  )
}
