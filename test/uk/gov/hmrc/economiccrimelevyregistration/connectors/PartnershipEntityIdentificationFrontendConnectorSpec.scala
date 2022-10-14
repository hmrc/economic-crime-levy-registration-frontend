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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.HttpClient
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.models.LimitedLiabilityPartnership

import scala.concurrent.Future

class PartnershipEntityIdentificationFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new PartnershipEntityIdentificationFrontendConnector(appConfig, mockHttpClient)
  val apiUrl                     = s"${appConfig.partnershipEntityIdentificationFrontendUrl}/partnership-identification/api"

  "createLimitedLiabilityPartnershipJourney" should {
    "return a GRS create journey response for the given request when the http client returns a GRS create journey response for the given request" in forAll {
      (grsCreateJourneyResponse: GrsCreateJourneyResponse) =>
        val expectedUrl = s"$apiUrl/limited-liability-partnership-journey"

        val expectedPartnershipEntityCreateJourneyRequest: PartnershipEntityCreateJourneyRequest = {
          val serviceNameLabels = ServiceNameLabels(
            En("Register for Economic Crime Levy"),
            Cy("service.name")
          )

          PartnershipEntityCreateJourneyRequest(
            continueUrl = "http://localhost:14000/register-for-economic-crime-levy/grs-continue",
            optServiceName = Some(serviceNameLabels.en.optServiceName),
            deskProServiceId = "economic-crime-levy-registration-frontend",
            signOutUrl = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
            accessibilityUrl = "/accessibility-statement/register-for-economic-crime-levy",
            labels = serviceNameLabels
          )
        }

        when(
          mockHttpClient.POST[PartnershipEntityCreateJourneyRequest, GrsCreateJourneyResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedPartnershipEntityCreateJourneyRequest),
            any()
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(grsCreateJourneyResponse))

        val result = await(connector.createPartnershipJourney(LimitedLiabilityPartnership))

        result shouldBe grsCreateJourneyResponse

        verify(mockHttpClient, times(1))
          .POST[PartnershipEntityCreateJourneyRequest, GrsCreateJourneyResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedPartnershipEntityCreateJourneyRequest),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

  "getJourneyData" should {
    "return journey data for a given journey id" in forAll {
      (partnershipEntityJourneyData: PartnershipEntityJourneyData, journeyId: String) =>
        val expectedUrl = s"$apiUrl/journey/$journeyId"

        when(
          mockHttpClient.GET[PartnershipEntityJourneyData](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())
        )
          .thenReturn(Future.successful(partnershipEntityJourneyData))

        val result = await(connector.getJourneyData(journeyId))

        result shouldBe partnershipEntityJourneyData

        verify(mockHttpClient, times(1))
          .GET[PartnershipEntityJourneyData](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }
}
