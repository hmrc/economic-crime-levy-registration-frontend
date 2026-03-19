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

package uk.gov.hmrc.economiccrimelevyregistration.generators

import org.scalacheck.Arbitrary
import io.github.martinhh.derived.scalacheck.deriveArbitrary
import play.api.mvc.Session
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.NotLiableReason.RevenueDoesNotMeetThreshold
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.{DeregisterReason, Deregistration}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.GroupEnrolmentsResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{DeregistrationRequestedEmailParameters, DeregistrationRequestedEmailRequest, RegistrationSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import org.scalacheck.Gen

object CachedArbitraries extends EclTestData {

  given arbRegistration: Arbitrary[Registration]                                                     = deriveArbitrary
  given arbBusinessSector: Arbitrary[BusinessSector]                                                 = deriveArbitrary
  given arbVerificationStatus: Arbitrary[VerificationStatus]                                         = deriveArbitrary
  given arbRegistrationStatus: Arbitrary[RegistrationStatus]                                         = deriveArbitrary
  given arbSubscriptionStatus: Arbitrary[SubscriptionStatus]                                         = deriveArbitrary
  given arbEntityType: Arbitrary[EntityType]                                                         = deriveArbitrary
  given arbAmlSupervisorType: Arbitrary[AmlSupervisorType]                                           = deriveArbitrary
  given arbIncorporatedEntityJourneyData: Arbitrary[IncorporatedEntityJourneyData]                   = deriveArbitrary
  given arbPartnershipEntityJourneyData: Arbitrary[PartnershipEntityJourneyData]                     = deriveArbitrary
  given arbSoleTraderEntityJourneyData: Arbitrary[SoleTraderEntityJourneyData]                       = deriveArbitrary
  given arbGrsCreateJourneyResponse: Arbitrary[GrsCreateJourneyResponse]                             = deriveArbitrary
  given arbGroupEnrolmentsResponse: Arbitrary[GroupEnrolmentsResponse]                               = deriveArbitrary
  given arbEclSubscriptionStatus: Arbitrary[EclSubscriptionStatus]                                   = deriveArbitrary
  given arbDataValidationError: Arbitrary[DataValidationError]                                       = deriveArbitrary
  given arbAlfAddressData: Arbitrary[AlfAddressData]                                                 = deriveArbitrary
  given arbCalculatedLiability: Arbitrary[CalculatedLiability]                                       = deriveArbitrary
  given arbCalculateLiabilityRequest: Arbitrary[CalculateLiabilityRequest]                           = deriveArbitrary
  given arbCreateEclSubscriptionResponse: Arbitrary[CreateEclSubscriptionResponse]                   = deriveArbitrary
  given arbRegistrationSubmittedParameters: Arbitrary[RegistrationSubmittedEmailParameters]          =
    deriveArbitrary
  given arbContacts: Arbitrary[Contacts]                                                             = deriveArbitrary
  given arbContactDetails: Arbitrary[ContactDetails]                                                 = deriveArbitrary
  given arbMode: Arbitrary[Mode]                                                                     = deriveArbitrary
  given arbRevenueDoesNotMeetThreshold: Arbitrary[RevenueDoesNotMeetThreshold]                       = deriveArbitrary
  given arbUtrType: Arbitrary[UtrType]                                                               = deriveArbitrary
  given arbAuthorization: Arbitrary[Authorization]                                                   =
    Arbitrary(Gen.alphaNumStr.suchThat(_.nonEmpty).map(Authorization.apply))
  given arbGetSubscriptionResponse: Arbitrary[GetSubscriptionResponse]                               = deriveArbitrary
  given arbGetAdditionalDetails: Arbitrary[GetAdditionalDetails]                                     = deriveArbitrary
  given arbEclAddress: Arbitrary[EclAddress]                                                         = deriveArbitrary
  given arbDeregisterReason: Arbitrary[DeregisterReason]                                             = deriveArbitrary
  given arbDeregistration: Arbitrary[Deregistration]                                                 = deriveArbitrary
  given arbGetCorrespondenceAddressDetails: Arbitrary[GetCorrespondenceAddressDetails]               =
    deriveArbitrary
  given arbRegistrationType: Arbitrary[RegistrationType]                                             = deriveArbitrary
  given arbEclRegistrationModel: Arbitrary[EclRegistrationModel]                                     = deriveArbitrary
  given arbDeregistrationRequestedEmailRequest: Arbitrary[DeregistrationRequestedEmailRequest]       =
    deriveArbitrary
  given arbDeregistrationRequestedEmailParameters: Arbitrary[DeregistrationRequestedEmailParameters] =
    deriveArbitrary
  given arbSession: Arbitrary[Session]                                                               = deriveArbitrary
  given arbExtendedDataEvent: Arbitrary[ExtendedDataEvent]                                           = deriveArbitrary
  given arbAuditResult: Arbitrary[AuditResult]                                                       = deriveArbitrary
  given arbGetLegalEntityDetails: Arbitrary[GetLegalEntityDetails]                                   = deriveArbitrary
  given arbGetPrimaryContactDetails: Arbitrary[GetPrimaryContactDetails]                             = deriveArbitrary
  given arbGetSecondaryContactDetails: Arbitrary[GetSecondaryContactDetails]                         = deriveArbitrary

}
