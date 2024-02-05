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

package uk.gov.hmrc.economiccrimelevyregistration.services

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, BusinessSector, ContactDetails, Contacts, EclAddress, GetSubscriptionResponse, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RegistrationStartedEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclRegistrationService @Inject() (
  eclRegistrationConnector: EclRegistrationConnector,
  auditConnector: AuditConnector,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {
  def getOrCreateRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[Registration] =
    eclRegistrationConnector.getRegistration(internalId).flatMap {
      case Some(registration) => Future.successful(registration)
      case None               =>
        auditConnector
          .sendExtendedEvent(
            RegistrationStartedEvent(
              internalId
            ).extendedDataEvent
          )

        eclRegistrationConnector.upsertRegistration(Registration.empty(internalId))
    }

  def upsertRegistration(registration: Registration)(implicit hc: HeaderCarrier): Future[Registration] =
    eclRegistrationConnector.upsertRegistration(registration)

  def deleteRegistration(internalId: String)(implicit hc: HeaderCarrier) =
    eclRegistrationConnector.deleteRegistration(internalId)

  def submitRegistration(internalId: String)(implicit hc: HeaderCarrier)                                    =
    eclRegistrationConnector.submitRegistration(internalId)
  def getSubscription(eclRegistration: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] =
    eclRegistrationConnector.getSubscription(eclRegistration)

  def transformToRegistration(
    registration: Registration,
    getSubscriptionResponse: GetSubscriptionResponse
  ): Registration = {
    val primaryContact      = getSubscriptionResponse.primaryContactDetails
    val secondaryContact    = getSubscriptionResponse.secondaryContactDetails
    val subscriptionAddress = getSubscriptionResponse.correspondenceAddressDetails

    val firstContactDetails: ContactDetails = ContactDetails(
      Some(primaryContact.name),
      Some(primaryContact.positionInCompany),
      Some(primaryContact.emailAddress),
      Some(primaryContact.telephone)
    )
    val secondContactDetails                = secondaryContact match {
      case Some(value) =>
        ContactDetails(
          Some(value.name),
          Some(value.positionInCompany),
          Some(value.emailAddress),
          Some(value.telephone)
        )
      case _           => ContactDetails.empty
    }
    val secondContactPresent                = Some(secondaryContact.isDefined)

    val contacts: Contacts = Contacts(firstContactDetails, secondContactPresent, secondContactDetails)

    val businessSector: BusinessSector =
      BusinessSector.transformFromSubscriptionResponse(getSubscriptionResponse.additionalDetails.businessSector)
    val address: EclAddress            = EclAddress(
      None,
      Some(subscriptionAddress.addressLine1),
      subscriptionAddress.addressLine2,
      subscriptionAddress.addressLine3,
      subscriptionAddress.addressLine4,
      None,
      subscriptionAddress.postCode,
      None,
      subscriptionAddress.countryCode.get
    )
    registration.copy(
      contacts = contacts,
      businessSector = Some(businessSector),
      contactAddress = Some(address),
      partnershipName = getSubscriptionResponse.legalEntityDetails.organisationName,
      amlSupervisor = Some(getAmlSupervisor(getSubscriptionResponse.additionalDetails.amlSupervisor))
    )
  }

  private def getAmlSupervisor(amlSupervisor: String): AmlSupervisor = {
    val sanitisedAmlSupervisor                         = amlSupervisor.filterNot(_.isWhitespace).toLowerCase
    val amlProfessionalBodySupervisors: Option[String] =
      appConfig.amlProfessionalBodySupervisors.find(p => p.toLowerCase() == sanitisedAmlSupervisor)

    if (amlProfessionalBodySupervisors.isEmpty) {
      sanitisedAmlSupervisor match {
        case "hmrc" => AmlSupervisor(AmlSupervisorType.Hmrc, None)
        case e      => throw new IllegalStateException(s"AML supervisor returned in GetSubscriptionResponse not valid: $e")
      }
    } else {
      AmlSupervisor(AmlSupervisorType.Other, Some(amlSupervisor))
    }
  }
}
