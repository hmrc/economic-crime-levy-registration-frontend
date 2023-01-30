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
import org.scalacheck.derive.MkArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.GroupEnrolmentsResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.email.RegistrationSubmittedEmailParameters
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary

object CachedArbitraries extends EclTestData {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbRegistration: Arbitrary[Registration]                                            = mkArb
  implicit lazy val arbBusinessSector: Arbitrary[BusinessSector]                                        = mkArb
  implicit lazy val arbVerificationStatus: Arbitrary[VerificationStatus]                                = mkArb
  implicit lazy val arbRegistrationStatus: Arbitrary[RegistrationStatus]                                = mkArb
  implicit lazy val arbSubscriptionStatus: Arbitrary[SubscriptionStatus]                                = mkArb
  implicit lazy val arbEntityType: Arbitrary[EntityType]                                                = mkArb
  implicit lazy val arbAmlSupervisorType: Arbitrary[AmlSupervisorType]                                  = mkArb
  implicit lazy val arbIncorporatedEntityJourneyData: Arbitrary[IncorporatedEntityJourneyData]          = mkArb
  implicit lazy val arbPartnershipEntityJourneyData: Arbitrary[PartnershipEntityJourneyData]            = mkArb
  implicit lazy val arbSoleTraderEntityJourneyData: Arbitrary[SoleTraderEntityJourneyData]              = mkArb
  implicit lazy val arbGrsCreateJourneyResponse: Arbitrary[GrsCreateJourneyResponse]                    = mkArb
  implicit lazy val arbGroupEnrolmentsResponse: Arbitrary[GroupEnrolmentsResponse]                      = mkArb
  implicit lazy val arbEclSubscriptionStatus: Arbitrary[EclSubscriptionStatus]                          = mkArb
  implicit lazy val arbDataValidationErrors: Arbitrary[DataValidationErrors]                            = mkArb
  implicit lazy val arbAlfAddressData: Arbitrary[AlfAddressData]                                        = mkArb
  implicit lazy val arbCalculatedLiability: Arbitrary[CalculatedLiability]                              = mkArb
  implicit lazy val arbCalculateLiabilityRequest: Arbitrary[CalculateLiabilityRequest]                  = mkArb
  implicit lazy val arbCreateEclSubscriptionResponse: Arbitrary[CreateEclSubscriptionResponse]          = mkArb
  implicit lazy val arbRegistrationSubmittedParameters: Arbitrary[RegistrationSubmittedEmailParameters] = mkArb
  implicit lazy val arbContacts: Arbitrary[Contacts]                                                    = mkArb

}
