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

import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, PartnershipEntityIdentificationFrontendConnector, SoleTraderEntityIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.{LimitedLiabilityPartnership, Registration, SoleTrader, UkLimitedCompany}
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

import scala.concurrent.Future

class GrsContinueControllerSpec extends SpecBase {
  val mockIncorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector =
    mock[IncorporatedEntityIdentificationFrontendConnector]

  val mockSoleTraderEntityIdentificationFrontendConnector: SoleTraderEntityIdentificationFrontendConnector =
    mock[SoleTraderEntityIdentificationFrontendConnector]

  val mockPartnershipEntityIdentificationFrontendConnector: PartnershipEntityIdentificationFrontendConnector =
    mock[PartnershipEntityIdentificationFrontendConnector]

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new GrsContinueController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockIncorporatedEntityIdentificationFrontendConnector,
      mockSoleTraderEntityIdentificationFrontendConnector,
      mockPartnershipEntityIdentificationFrontendConnector,
      mockEclRegistrationConnector
    )
  }
  "continue" should {

    "retrieve the incorporated entity GRS journey data and display the GRS result" in forAll {
      (journeyId: String, registration: Registration, incorporatedEntityJourneyData: IncorporatedEntityJourneyData) =>
        new TestContext(registration.copy(entityType = Some(UkLimitedCompany))) {
          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(incorporatedEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(incorporatedEntityJourneyData),
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )
          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsJson(result) shouldBe Json.toJson(incorporatedEntityJourneyData)
        }
    }

    "retrieve the sole trader entity GRS journey data and display the GRS result" in forAll {
      (journeyId: String, registration: Registration, soleTraderEntityJourneyData: SoleTraderEntityJourneyData) =>
        new TestContext(registration.copy(entityType = Some(SoleTrader))) {
          when(
            mockSoleTraderEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
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
      (journeyId: String, registration: Registration, partnershipEntityJourneyData: PartnershipEntityJourneyData) =>
        new TestContext(registration.copy(entityType = Some(LimitedLiabilityPartnership))) {
          when(
            mockPartnershipEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(partnershipEntityJourneyData))

          val updatedRegistration: Registration = registration.copy(
            entityType = Some(LimitedLiabilityPartnership),
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

  }
}
