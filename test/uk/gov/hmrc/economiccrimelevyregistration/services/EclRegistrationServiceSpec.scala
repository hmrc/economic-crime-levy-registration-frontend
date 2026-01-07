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

import cats.data.OptionT
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.scalacheck.Arbitrary
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyregistration.{IncorporatedEntityType, LimitedPartnershipType}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{GeneralPartnership, LimitedLiabilityPartnership, LimitedPartnership, RegisteredSociety, ScottishLimitedPartnership, ScottishPartnership, SoleTrader, UkLimitedCompany, UnlimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, BusinessSector, ContactDetails, Contacts, CreateEclSubscriptionResponse, EclAddress, EclSubscriptionStatus, EntityType, GetAdditionalDetails, GetSubscriptionResponse, Mode, Registration}
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}
import org.mockito.Mockito.{reset, times, verify, when}

import scala.concurrent.Future
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.GrsCreateJourneyResponse

import java.net.URL

class EclRegistrationServiceSpec extends SpecBase {
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val mockAuditService: AuditService                         = mock[AuditService]
  val mockIncorporatedEntityIdentificationFrontendConnector  = mock[IncorporatedEntityIdentificationFrontendConnector]
  val mockSoleTraderIdentificationFrontendConnector          = mock[SoleTraderIdentificationFrontendConnector]
  val mockPartnershipIdentificationFrontendConnector         = mock[PartnershipIdentificationFrontendConnector]
  val service                                                = new EclRegistrationService(
    mockEclRegistrationConnector,
    mockIncorporatedEntityIdentificationFrontendConnector,
    mockSoleTraderIdentificationFrontendConnector,
    mockPartnershipIdentificationFrontendConnector,
    mockAuditService,
    appConfig
  )

  "getOrCreateRegistration" should {
    "return a created registration when one does not exist" in forAll { (internalId: String) =>
      val emptyRegistration = Registration.empty(internalId).copy(registrationType = Some(Initial))
      when(mockEclRegistrationConnector.getRegistration(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND)))

      when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
        .thenReturn(Future.successful(()))

      val result = await(service.getOrCreate(internalId).value)
      result shouldBe Right(emptyRegistration)
    }

    "return an existing registration" in forAll { (internalId: String, registration: Registration) =>
      when(mockEclRegistrationConnector.getRegistration(any())(any()))
        .thenReturn(Future.successful(registration))

      val result = await(service.getOrCreate(internalId).value)
      result shouldBe Right(registration)

    }
  }

  "getSubscription" should {
    "return valid subscription response when eclReference is passed" in forAll(
      Arbitrary.arbitrary[GetSubscriptionResponse],
      nonEmptyString
    ) { (getSubscriptionResponse: GetSubscriptionResponse, eclReference: String) =>
      when(mockEclRegistrationConnector.getSubscription(eclReference))
        .thenReturn(Future.successful(getSubscriptionResponse))

      val result = await(service.getSubscription(eclReference).value)

      result shouldBe Right(getSubscriptionResponse)
    }

    "return error when call to connector fails with an Upstream5xxResponse" in forAll(
      nonEmptyString
    ) { (eclReference: String) =>
      when(mockEclRegistrationConnector.getSubscription(ArgumentMatchers.eq(eclReference))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

      val result = await(
        service
          .getSubscription(eclReference)
          .value
      )
      result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return error when call to connector fails with an Upstream4xxResponse" in forAll(
      nonEmptyString
    ) { (eclReference: String) =>
      when(mockEclRegistrationConnector.getSubscription(ArgumentMatchers.eq(eclReference))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", NOT_FOUND)))

      val result = await(
        service
          .getSubscription(eclReference)
          .value
      )
      result shouldBe Left(DataRetrievalError.BadGateway("Error", NOT_FOUND))
    }

    "return an InternalUnexpectedError when a non fatal exception is thrown" in {

      val exception = new NullPointerException("Null Pointer Exception")
      when(mockEclRegistrationConnector.getSubscription(ArgumentMatchers.eq(testEclRegistrationReference))(any()))
        .thenReturn(Future.failed(exception))

      val result = await(service.getSubscription(testEclRegistrationReference).value)
      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

  }

  "upsertRegistration" should {
    "return unit when registration is upserted successfully" in forAll { (registration: Registration) =>
      when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(registration))(any()))
        .thenReturn(Future.successful(Right(())))

      val result = await(service.upsertRegistration(registration).value)

      result shouldBe Right(())
    }

    "return an error when the upsert fails" in forAll { (registration: Registration) =>
      when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(registration))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

      val result = await(service.upsertRegistration(registration).value)

      result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return an error when the upsert fails with an Upstream4xxResponse" in forAll { (registration: Registration) =>
      when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(registration))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", BAD_REQUEST)))

      val result = await(service.upsertRegistration(registration).value)

      result shouldBe Left(DataRetrievalError.BadGateway("Error", BAD_REQUEST))
    }

    "return an DataRetrievalError.InternalUnexpectedError when an exception is thrown" in forAll {
      (registration: Registration) =>
        val exception = new NullPointerException("NullPointerEx")
        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(registration))(any()))
          .thenReturn(Future.failed(exception))

        val result = await(service.upsertRegistration(registration).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

  }

  "submitRegistration" should {
    "return a CreateEclSubscriptionResponse when the registration is submitted successfully" in forAll {
      (subscriptionResponse: CreateEclSubscriptionResponse) =>
        when(mockEclRegistrationConnector.submitRegistration(ArgumentMatchers.eq(testInternalId))(any()))
          .thenReturn(Future.successful(subscriptionResponse))

        val result = await(service.submitRegistration(testInternalId).value)

        result shouldBe Right(subscriptionResponse)
    }

    "return an error when the submission fails" in {
      when(mockEclRegistrationConnector.submitRegistration(ArgumentMatchers.eq(testInternalId))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

      val result = await(service.submitRegistration(testInternalId).value)

      result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }
  }

  "getRegistrationValidationErrors" should {
    "return Some and a DataValidationError when a DataValidationError is present in the registration" in forAll {
      (error: String) =>
        val updatedDataValidationError = DataValidationError(error)
        when(mockEclRegistrationConnector.getRegistrationValidationErrors(ArgumentMatchers.eq(testInternalId))(any()))
          .thenReturn(OptionT[Future, String](Future.successful(Some(error))))

        val result = await(service.getRegistrationValidationErrors(testInternalId).value)

        result shouldBe Right(Some(updatedDataValidationError))
    }

    "return a Left(DataRetrievalError.BadGateway) when the call to the connector fails" in {
      when(mockEclRegistrationConnector.getRegistrationValidationErrors(ArgumentMatchers.eq(testInternalId))(any()))
        .thenReturn(OptionT[Future, String](Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR))))

      val result = await(service.getRegistrationValidationErrors(testInternalId).value)

      result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return a Left(DataRetrievalError.InternalUnexpectedError) if an exception is thrown" in {
      val exception = new NullPointerException("NullPointerException")
      when(mockEclRegistrationConnector.getRegistrationValidationErrors(ArgumentMatchers.eq(testInternalId))(any()))
        .thenReturn(OptionT[Future, String](Future.failed(exception)))

      val result = await(service.getRegistrationValidationErrors(testInternalId).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }
  }

  "transformToRegistration" should {
    "return a registration from a GetSubscriptionResponse" in forAll {
      (
        registration: Registration,
        getSubscriptionResponse: GetSubscriptionResponse,
        regDate: String,
        startDate: String,
        businessSector: BusinessSector
      ) =>
        val amlSupervisor               = "HMRC"
        val businessSectorAsString      = BusinessSector.transformFromSubscriptionResponse(businessSector.toString).toString
        val additionalDetails           = GetAdditionalDetails(
          registrationDate = regDate,
          liabilityStartDate = startDate,
          eclReference = testEclRegistrationReference,
          amlSupervisor = amlSupervisor,
          businessSector = businessSectorAsString
        )
        val updatedSubscriptionResponse = getSubscriptionResponse.copy(
          additionalDetails = additionalDetails,
          correspondenceAddressDetails =
            getSubscriptionResponse.correspondenceAddressDetails.copy(countryCode = Some(alphaNumericString))
        )
        val primaryContact              = updatedSubscriptionResponse.primaryContactDetails
        val secondaryContact            = updatedSubscriptionResponse.secondaryContactDetails
        val subscriptionAddress         = updatedSubscriptionResponse.correspondenceAddressDetails

        val secondContactPresent = Some(secondaryContact.isDefined)

        val firstContactDetails = ContactDetails(
          Some(primaryContact.name),
          Some(primaryContact.positionInCompany),
          Some(primaryContact.emailAddress),
          Some(primaryContact.telephone)
        )

        val secondContactDetails = secondaryContact match {
          case Some(value) =>
            ContactDetails(
              Some(value.name),
              Some(value.positionInCompany),
              Some(value.emailAddress),
              Some(value.telephone)
            )
          case _           => ContactDetails.empty
        }

        val contacts = Contacts(firstContactDetails, secondContactPresent, secondContactDetails)

        val address = EclAddress(
          None,
          Some(subscriptionAddress.addressLine1),
          subscriptionAddress.addressLine2,
          subscriptionAddress.addressLine3,
          subscriptionAddress.addressLine4,
          None,
          subscriptionAddress.postCode,
          None,
          Some(subscriptionAddress.countryCode.get)
        )

        val registrationFromSubscriptionResponse = registration.copy(
          contacts = contacts,
          businessSector = Some(
            BusinessSector.transformFromSubscriptionResponse(
              updatedSubscriptionResponse.additionalDetails.businessSector
            )
          ),
          contactAddress = Some(address),
          amlSupervisor = Some(AmlSupervisor(AmlSupervisorType.Hmrc, None))
        )

        val result = service.transformToRegistration(registration, updatedSubscriptionResponse)

        result shouldBe registrationFromSubscriptionResponse

    }
  }

  "registerEntityType" should {
    "return a string containing the journey start url for an incorporated entity type" in forAll {
      (journeyResponse: GrsCreateJourneyResponse, mode: Mode) =>
        val entityType  = random[IncorporatedEntityType].entityType
        val apiUrl: URL =
          url"${appConfig.incorporatedEntityIdentificationFrontendBaseUrl}/incorporated-entity-identification/api"

        val url: URL               = entityType match {
          case UkLimitedCompany | UnlimitedCompany => url"$apiUrl/limited-company-journey"
          case RegisteredSociety                   => url"$apiUrl/registered-society-journey"
          case entityType: EntityType              => fail(s"Invalid entityType $entityType")
        }
        val updatedJourneyResponse = journeyResponse.copy(journeyStartUrl = url.toString)

        when(
          mockIncorporatedEntityIdentificationFrontendConnector
            .createIncorporatedEntityJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(any())
        )
          .thenReturn(Future.successful(updatedJourneyResponse))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Right(updatedJourneyResponse.journeyStartUrl)
    }

    "return an error if call to incorporatedEntityIdentificationConnector fails for an incorporated entity type" in forAll {
      (mode: Mode) =>
        val entityType = random[IncorporatedEntityType].entityType

        when(
          mockIncorporatedEntityIdentificationFrontendConnector
            .createIncorporatedEntityJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND)))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Left(DataRetrievalError.BadGateway("Not found", NOT_FOUND))
    }

    "return an InternalUnexpectedError if call to incorporatedEntityIdentificationConnector throws an exception" in forAll {
      (mode: Mode) =>
        val entityType = random[IncorporatedEntityType].entityType
        val exception  = new NullPointerException("Null Pointer Exception")

        when(
          mockIncorporatedEntityIdentificationFrontendConnector
            .createIncorporatedEntityJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(any())
        )
          .thenReturn(Future.failed(exception))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

    "return a string containing the journey start url for an Sole Trader entity type" in forAll {
      (journeyResponse: GrsCreateJourneyResponse, mode: Mode) =>
        val entityType  = SoleTrader
        val apiUrl: URL =
          url"${appConfig.soleTraderEntityIdentificationFrontendBaseUrl}/sole-trader-identification/api"
        val url         = url"$apiUrl/sole-trader-journey"

        val updatedJourneyResponse = journeyResponse.copy(journeyStartUrl = url.toString)

        when(mockSoleTraderIdentificationFrontendConnector.createSoleTraderJourney(ArgumentMatchers.eq(mode))(any()))
          .thenReturn(Future.successful(updatedJourneyResponse))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Right(updatedJourneyResponse.journeyStartUrl)
    }

    "return an error if call to soleTraderIdentificationFrontendConnector fails for an incorporated entity type" in forAll {
      (mode: Mode) =>
        val entityType = SoleTrader

        when(mockSoleTraderIdentificationFrontendConnector.createSoleTraderJourney(ArgumentMatchers.eq(mode))(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND)))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Left(DataRetrievalError.BadGateway("Not found", NOT_FOUND))
    }

    "return an InternalUnexpectedError if call to soleTraderIdentificationFrontendConnector throws an exception" in forAll {
      (mode: Mode) =>
        val exception  = new NullPointerException("Null Pointer Exception")
        val entityType = SoleTrader

        when(mockSoleTraderIdentificationFrontendConnector.createSoleTraderJourney(ArgumentMatchers.eq(mode))(any()))
          .thenReturn(Future.failed(exception))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

    "return a string containing the journey start url for an Partnership entity type" in forAll {
      (journeyResponse: GrsCreateJourneyResponse, mode: Mode) =>
        val entityType  = random[LimitedPartnershipType].entityType
        val apiUrl: URL =
          url"${appConfig.partnershipEntityIdentificationFrontendBaseUrl}/partnership-identification/api"

        val url: URL = entityType match {
          case GeneralPartnership          => url"$apiUrl/general-partnership-journey"
          case ScottishPartnership         => url"$apiUrl/scottish-partnership-journey"
          case LimitedPartnership          => url"$apiUrl/limited-partnership-journey"
          case ScottishLimitedPartnership  => url"$apiUrl/scottish-limited-partnership-journey"
          case LimitedLiabilityPartnership => url"$apiUrl/limited-liability-partnership-journey"
          case entityType: EntityType      => fail(s"Invalid entityType $entityType")
        }

        val updatedJourneyResponse = journeyResponse.copy(journeyStartUrl = url.toString)

        when(
          mockPartnershipIdentificationFrontendConnector
            .createPartnershipJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(any())
        )
          .thenReturn(Future.successful(updatedJourneyResponse))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Right(updatedJourneyResponse.journeyStartUrl)
    }

    "return an error if call to partnershipIdentificationFrontEndConnector fails" in forAll { (mode: Mode) =>
      val entityType = random[LimitedPartnershipType].entityType

      when(
        mockPartnershipIdentificationFrontendConnector
          .createPartnershipJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(any())
      )
        .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND)))

      val result = await(service.registerEntityType(entityType, mode).value)

      result shouldBe Left(DataRetrievalError.BadGateway("Not found", NOT_FOUND))
    }

    "return an InternalUnexpectedError if call to partnershipIdentificationFrontendConnector throws an exception" in forAll {
      (mode: Mode) =>
        val exception  = new NullPointerException("Null Pointer Exception")
        val entityType = random[LimitedPartnershipType].entityType

        when(
          mockPartnershipIdentificationFrontendConnector
            .createPartnershipJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(any())
        )
          .thenReturn(Future.failed(exception))

        val result = await(service.registerEntityType(entityType, mode).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

  }

  "getSubscriptionStatus" should {
    "return an EclSubscriptionStatus" in forAll {
      (subscriptionStatus: EclSubscriptionStatus, businessPartnerId: String) =>
        when(mockEclRegistrationConnector.getSubscriptionStatus(anyString())(any()))
          .thenReturn(Future.successful(subscriptionStatus))

        val result = await(service.getSubscriptionStatus(businessPartnerId).value)

        result shouldBe Right(subscriptionStatus)
    }

    "return a Left(DataRetrievalError.BadGateway) when the connector fails with a Upstream5xxResponse" in forAll {
      (businessPartnerId: String) =>
        when(mockEclRegistrationConnector.getSubscriptionStatus(anyString())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

        val result = await(service.getSubscriptionStatus(businessPartnerId).value)

        result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return a Left(DataRetrievalError.BadGateway) when the connector fails with a Upstream4xxResponse" in forAll {
      (businessPartnerId: String) =>
        when(mockEclRegistrationConnector.getSubscriptionStatus(anyString())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Error", NOT_FOUND)))

        val result = await(service.getSubscriptionStatus(businessPartnerId).value)

        result shouldBe Left(DataRetrievalError.BadGateway("Error", NOT_FOUND))
    }

    "return a Left(DataRetrievalError.InternalUnexpectedError) if an exception is thrown" in forAll {
      (businessPartnerId: String) =>
        val exception = new NullPointerException("NullPointerException")
        when(mockEclRegistrationConnector.getSubscriptionStatus(anyString())(any()))
          .thenReturn(Future.failed(exception))

        val result = await(service.getSubscriptionStatus(businessPartnerId).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }
  }
}
