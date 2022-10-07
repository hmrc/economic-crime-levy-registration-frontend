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

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, SoleTraderEntityIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, SoleTraderEntityJourneyData}
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, SoleTrader, UkLimitedCompany}

import scala.concurrent.Future

class GrsContinueControllerSpec extends SpecBase {
  val mockIncorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector =
    mock[IncorporatedEntityIdentificationFrontendConnector]

  val mockSoleTraderEntityIdentificationFrontendConnector: SoleTraderEntityIdentificationFrontendConnector =
    mock[SoleTraderEntityIdentificationFrontendConnector]

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new GrsContinueController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockIncorporatedEntityIdentificationFrontendConnector,
      mockSoleTraderEntityIdentificationFrontendConnector,
      mockEclRegistrationConnector
    )
  }
  "continue" should {

    "retrieve the incorporated entity GRS journey data and display the GRS result" in forAll {
      (journeyId: String, incorporatedEntityJourneyData: IncorporatedEntityJourneyData) =>
        new TestContext(testRegistration.copy(entityType = Some(UkLimitedCompany))) {
          when(
            mockIncorporatedEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(incorporatedEntityJourneyData))

          val updatedRegistration: Registration = testRegistration.copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = Some(incorporatedEntityJourneyData)
          )
          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsJson(result) shouldBe Json.toJson(incorporatedEntityJourneyData)
        }
    }

    "retrieve the sole trader entity GRS journey data and display the GRS result" in forAll {
      (journeyId: String, soleTraderEntityJourneyData: SoleTraderEntityJourneyData) =>
        new TestContext(testRegistration.copy(entityType = Some(SoleTrader))) {
          when(
            mockSoleTraderEntityIdentificationFrontendConnector.getJourneyData(ArgumentMatchers.eq(journeyId))(any())
          )
            .thenReturn(Future.successful(soleTraderEntityJourneyData))

          val updatedRegistration: Registration = testRegistration.copy(
            entityType = Some(SoleTrader),
            soleTraderEntityJourneyData = Some(soleTraderEntityJourneyData)
          )
          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(journeyId)(fakeRequest)

          status(result) shouldBe OK

          contentAsJson(result) shouldBe Json.toJson(soleTraderEntityJourneyData)
        }
    }

  }
}
