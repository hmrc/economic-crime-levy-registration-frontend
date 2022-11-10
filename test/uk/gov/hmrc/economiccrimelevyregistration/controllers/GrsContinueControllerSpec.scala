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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.PartnershipType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

import scala.concurrent.Future

class GrsContinueControllerSpec extends SpecBase {
  val mockIncorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector =
    mock[IncorporatedEntityIdentificationFrontendConnector]

  val mockSoleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector =
    mock[SoleTraderIdentificationFrontendConnector]

  val mockPartnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector =
    mock[PartnershipIdentificationFrontendConnector]

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new GrsContinueController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockIncorporatedEntityIdentificationFrontendConnector,
      mockSoleTraderIdentificationFrontendConnector,
      mockPartnershipIdentificationFrontendConnector,
      mockEclRegistrationConnector
    )
  }

  "continue" should {
    "retrieve the incorporated entity GRS journey data and continue if registration was successful and the business partner is not already subscribed to ECL" in forAll {
      (
        journeyId: String,
        registration: Registration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
        businessPartnerId: String
      ) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(
              identifiersMatch = true,
              registration = successfulGrsRegistrationResult(businessPartnerId),
              businessVerification = None
            )

          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

          when(
            mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
          ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe "Success - you can continue registering for ECL"
        }
    }

    "retrieve the incorporated entity GRS journey data and display the BV failed result if the business verification status is FAIL" in forAll {
      (
        journeyId: String,
        registration: Registration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData
      ) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(identifiersMatch = true, businessVerification = failedBvResult)

          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe "Failed business verification"
        }
    }

    "retrieve the incorporated entity GRS journey data and display the registration failed result if the registration status is REGISTRATION_FAILED" in forAll {
      (
        journeyId: String,
        registration: Registration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData
      ) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(
              identifiersMatch = true,
              registration = failedRegistrationResult,
              businessVerification = None
            )

          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe "Registration failed"
        }
    }

    "retrieve the incorporated entity GRS journey data and display the matching failed result if identifiers match is false" in forAll {
      (
        journeyId: String,
        registration: Registration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData
      ) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(identifiersMatch = false)

          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe "Identifiers do not match"
        }
    }

    "retrieve the incorporated entity GRS journey data and display the already subscribed result if the business partner is already subscribed to ECL" in forAll {
      (
        journeyId: String,
        registration: Registration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
        businessPartnerId: String,
        eclRegistrationReference: String
      ) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(
              identifiersMatch = true,
              registration = successfulGrsRegistrationResult(businessPartnerId),
              businessVerification = None
            )

          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

          when(
            mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
          ).thenReturn(Future.successful(EclSubscriptionStatus(Subscribed(eclRegistrationReference))))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(
            result
          ) shouldBe s"Business is already subscribed to ECL with registration reference $eclRegistrationReference"
        }
    }

    "retrieve the sole trader entity GRS journey data and display the GRS result" in forAll {
      (journeyId: String, registration: Registration, soleTraderEntityJourneyData: SoleTraderEntityJourneyData) =>
        new TestContext(registration.copy(entityType = Some(SoleTrader))) {
          when(
            mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(soleTraderEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(SoleTrader),
            soleTraderEntityJourneyData = Some(soleTraderEntityJourneyData),
            incorporatedEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )
          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsJson(result) shouldBe Json.toJson(soleTraderEntityJourneyData)
        }
    }

    "retrieve the partnership entity GRS journey data and display the GRS result" in forAll {
      (
        journeyId: String,
        registration: Registration,
        partnershipEntityJourneyData: PartnershipEntityJourneyData,
        partnershipType: PartnershipType
      ) =>
        val entityType = partnershipType.entityType
        new TestContext(registration.copy(entityType = Some(entityType))) {
          when(
            mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(partnershipEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(entityType),
            partnershipEntityJourneyData = Some(partnershipEntityJourneyData),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = None
          )

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsJson(result) shouldBe Json.toJson(partnershipEntityJourneyData)
        }
    }

    "throw an IllegalStateException if there is no entity type data in the registration" in forAll {
      (journeyId: String, registration: Registration) =>
        new TestContext(registration.copy(entityType = None)) {

          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.continue(journeyId)(fakeRequest))
          }

          result.getMessage shouldBe "No entity type found in registration data"
        }
    }

  }
}
