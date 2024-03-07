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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.economiccrimelevyregistration.{IncorporatedEntityType, LimitedPartnershipType, PartnershipType, ScottishOrGeneralPartnershipType}

import scala.concurrent.Future

class GrsContinueControllerSpec extends SpecBase with BaseController {
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
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      fakeStoreUrlAction(),
      mockIncorporatedEntityIdentificationFrontendConnector,
      mockSoleTraderIdentificationFrontendConnector,
      mockPartnershipIdentificationFrontendConnector,
      mockEclRegistrationConnector
    )
  }

  "continue" should {
    "retrieve the incorporated entity GRS journey data and continue to the next page in normal mode if registration " +
      "was successful and the business partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType,
          businessPartnerId: String
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
          }
      }

    "retrieve the incorporated entity GRS journey data and go to the check your answers page in check mode if " +
      "registration was successful and the business partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType,
          businessPartnerId: String
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(CheckMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
          }
      }

    "retrieve the incorporated entity GRS journey data and display the BV failed result if the business " +
      "verification status is FAIL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(identifiersMatch = true, businessVerification = failedBvResult)

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.verificationFailed().url)
          }
      }

    "retrieve the incorporated entity GRS journey data and display the party type mismatch result if the registration " +
      "status is REGISTRATION_FAILED and the failures contains the PARTY_TYPE_MISMATCH failure" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(
                identifiersMatch = true,
                registration = partyTypeMismatchResult,
                businessVerification = None
              )

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.partyTypeMismatch().url)
          }
      }

    "retrieve the incorporated entity GRS journey data and display the registration failed result if the registration" +
      " status is REGISTRATION_FAILED" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(
                identifiersMatch = true,
                registration = failedRegistrationResult,
                businessVerification = None
              )

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.registrationFailed().url)
          }
      }

    "retrieve the incorporated entity GRS journey data and display the matching failed result if identifiers " +
      "match is false" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(identifiersMatch = false)

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.verificationFailed().url)
          }
      }

    "retrieve the incorporated entity GRS journey data and display the already registered page if the business " +
      "partner is already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
          incorporatedEntityType: IncorporatedEntityType,
          businessPartnerId: String,
          eclRegistrationReference: String
        ) =>
          new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
            val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
              incorporatedEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
                any()
              )
            )
              .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(incorporatedEntityType.entityType),
              incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(Subscribed(eclRegistrationReference))))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(
              routes.NotableErrorController.organisationAlreadyRegistered(eclRegistrationReference).url
            )
          }
      }

    "retrieve the incorporated entity GRS journey data and show error if the business partner is deregistered ECL" in forAll {
      (
        journeyId: String,
        registration: Registration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
        incorporatedEntityType: IncorporatedEntityType,
        businessPartnerId: String,
        eclRegistrationReference: String
      ) =>
        new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(
              identifiersMatch = true,
              registration = successfulGrsRegistrationResult(businessPartnerId),
              businessVerification = None
            )

          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(
              any()
            )
          )
            .thenReturn(Future.successful(updatedIncorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(incorporatedEntityType.entityType),
            incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

          when(
            mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
          ).thenReturn(Future.successful(EclSubscriptionStatus(DeRegistered(eclRegistrationReference))))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR

//          redirectLocation(result) shouldBe Some(
//            routes.NotableErrorController.organisationAlreadyRegistered(eclRegistrationReference).url
//          )
        }
    }

    "retrieve the sole trader entity GRS journey data and continue to the next page in normal mode if registration " +
      "was successful and the business partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData,
          businessPartnerId: String
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              incorporatedEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
          }
      }

    "retrieve the sole trader entity GRS journey data and go to the check your answers page in check mode if " +
      "registration was successful and the business partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData,
          businessPartnerId: String
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              incorporatedEntityJourneyData = None,
              partnershipEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(CheckMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
          }
      }

    "retrieve the sole trader entity GRS journey data and display the BV failed result if the business verification " +
      "status is FAIL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(identifiersMatch = true, businessVerification = failedBvResult)

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.verificationFailed().url)
          }
      }

    "retrieve the sole trader entity GRS journey data and display the party type mismatch result if the registration " +
      "status is REGISTRATION_FAILED and the failures contains the PARTY_TYPE_MISMATCH failure" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(
                identifiersMatch = true,
                registration = partyTypeMismatchResult,
                businessVerification = None
              )

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.partyTypeMismatch().url)
          }
      }

    "retrieve the sole trader entity GRS journey data and display the registration failed result if the registration " +
      "status is REGISTRATION_FAILED" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(
                identifiersMatch = true,
                registration = failedRegistrationResult,
                businessVerification = None
              )

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.registrationFailed().url)
          }
      }

    "retrieve the sole trader entity GRS journey data and display the matching failed result if identifiers " +
      "match is false" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(identifiersMatch = false)

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              partnershipEntityJourneyData = None
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.verificationFailed().url)
          }
      }

    "retrieve the sole trader entity GRS journey data and display the already registered page if the business " +
      "partner is already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          soleTraderEntityJourneyData: SoleTraderEntityJourneyData,
          businessPartnerId: String,
          eclRegistrationReference: String
        ) =>
          new TestContext(registration.copy(entityType = Some(SoleTrader))) {
            val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
              soleTraderEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(SoleTrader),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
              partnershipEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(Subscribed(eclRegistrationReference))))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(
              routes.NotableErrorController.organisationAlreadyRegistered(eclRegistrationReference).url
            )
          }
      }

    "retrieve the sole trader entity GRS journey data and show error if the business partner is deregistered ECL" in forAll {
      (
        journeyId: String,
        registration: Registration,
        soleTraderEntityJourneyData: SoleTraderEntityJourneyData,
        businessPartnerId: String,
        eclRegistrationReference: String
      ) =>
        new TestContext(registration.copy(entityType = Some(SoleTrader))) {
          val updatedSoleTraderEntityJourneyData: SoleTraderEntityJourneyData =
            soleTraderEntityJourneyData.copy(
              identifiersMatch = true,
              registration = successfulGrsRegistrationResult(businessPartnerId),
              businessVerification = None
            )

          when(
            mockSoleTraderIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedSoleTraderEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(SoleTrader),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData),
            partnershipEntityJourneyData = None
          )

          when(
            mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
          ).thenReturn(Future.successful(EclSubscriptionStatus(DeRegistered(eclRegistrationReference))))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "retrieve the partnership entity GRS journey data for a limited liability, limited and scottish limited " +
      "partnership and continue to the business sector page in normal mode if registration was successful and " +
      "the business partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: LimitedPartnershipType,
          businessPartnerId: String
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
          }
      }

    "retrieve the partnership entity GRS journey data for a general and scottish partnership and " +
      "continue to the partnership name page in normal mode if registration was successful and the business " +
      "partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: ScottishOrGeneralPartnershipType,
          businessPartnerId: String
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.PartnershipNameController.onPageLoad(NormalMode).url)
          }
      }

    "retrieve the partnership entity GRS journey data and go to the check your answers page in check mode if " +
      "registration was successful and the business partner is not already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: PartnershipType,
          businessPartnerId: String,
          partnershipName: String
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(
            registration.copy(
              entityType = Some(entityType),
              partnershipName = Some(partnershipName)
            )
          ) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None,
              partnershipName = Some(partnershipName)
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(CheckMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
          }
      }

    "retrieve the partnership entity GRS journey data and display the BV failed result if the business " +
      "verification status is FAIL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: PartnershipType
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(identifiersMatch = true, businessVerification = failedBvResult)

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.verificationFailed().url)
          }
      }

    "retrieve the partnership entity GRS journey data and display the party type mismatch result if the " +
      "registration status is REGISTRATION_FAILED and the failures contains the PARTY_TYPE_MISMATCH failure" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: PartnershipType
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(
                identifiersMatch = true,
                registration = partyTypeMismatchResult,
                businessVerification = None
              )

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.partyTypeMismatch().url)
          }
      }

    "retrieve the partnership entity GRS journey data and display the registration failed result if the " +
      "registration status is REGISTRATION_FAILED" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: PartnershipType
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(
                identifiersMatch = true,
                registration = failedRegistrationResult,
                businessVerification = None
              )

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.registrationFailed().url)
          }
      }

    "retrieve the partnership entity GRS journey data and display the matching failed result if identifiers " +
      "match is false" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: PartnershipType
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(identifiersMatch = false)

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
            )

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(routes.NotableErrorController.verificationFailed().url)
          }
      }

    "retrieve the partnership entity GRS journey data and display the already registered page if the business " +
      "partner is already subscribed to ECL" in forAll {
        (
          journeyId: String,
          registration: Registration,
          partnershipEntityJourneyData: PartnershipEntityJourneyData,
          partnershipType: PartnershipType,
          businessPartnerId: String,
          eclRegistrationReference: String
        ) =>
          val entityType = partnershipType.entityType
          new TestContext(registration.copy(entityType = Some(entityType))) {
            val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
              partnershipEntityJourneyData.copy(
                identifiersMatch = true,
                registration = successfulGrsRegistrationResult(businessPartnerId),
                businessVerification = None
              )

            when(
              mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
            )
              .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

            val updatedRegistration: Registration = registration.copy(
              entityType = Some(entityType),
              incorporatedEntityJourneyData = None,
              soleTraderEntityJourneyData = None,
              partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
            )

            when(
              mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
            ).thenReturn(Future.successful(EclSubscriptionStatus(Subscribed(eclRegistrationReference))))

            when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
              .thenReturn(Future.successful(()))

            val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(
              routes.NotableErrorController.organisationAlreadyRegistered(eclRegistrationReference).url
            )
          }
      }

    "return an error if there is no entity type data in the registration" in forAll {
      (journeyId: String, registration: Registration) =>
        new TestContext(registration.copy(entityType = None)) {

          val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
        }
    }

    "throw an IllegalStateException if an invalid result is received from GRS" in forAll {
      (journeyId: String, incorporatedEntityJourneyData: IncorporatedEntityJourneyData, registration: Registration) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          val identifiersMatch: Boolean                = true
          val noBv: Option[BusinessVerificationResult] = None

          val updatedIncorporatedEntityJourneyData: IncorporatedEntityJourneyData =
            incorporatedEntityJourneyData.copy(
              identifiersMatch = identifiersMatch,
              registration = registrationNotCalled,
              businessVerification = noBv
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
            .thenReturn(Future.successful(()))

          val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "redirect to partnership name pge in check mode if no name in registration" in forAll(
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[PartnershipEntityJourneyData],
      Arbitrary
        .arbitrary[PartnershipType]
        .retryUntil(p => Seq(GeneralPartnership, ScottishPartnership).contains(p.entityType)),
      Arbitrary.arbitrary[String]
    ) {
      (
        journeyId: String,
        registration: Registration,
        partnershipEntityJourneyData: PartnershipEntityJourneyData,
        partnershipType: PartnershipType,
        businessPartnerId: String
      ) =>
        val entityType = partnershipType.entityType
        new TestContext(
          registration.copy(
            entityType = Some(entityType),
            partnershipName = None
          )
        ) {
          val updatedPartnershipEntityJourneyData: PartnershipEntityJourneyData =
            partnershipEntityJourneyData.copy(
              identifiersMatch = true,
              registration = successfulGrsRegistrationResult(businessPartnerId),
              businessVerification = None
            )

          when(
            mockPartnershipIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(updatedPartnershipEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(entityType),
            partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = None,
            partnershipName = None
          )

          when(
            mockEclRegistrationConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any())
          ).thenReturn(Future.successful(EclSubscriptionStatus(NotSubscribed)))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] = controller.continue(CheckMode, journeyId)(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.PartnershipNameController.onPageLoad(CheckMode).url)
        }
    }
  }
}
