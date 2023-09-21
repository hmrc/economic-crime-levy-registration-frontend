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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{RegisteredSociety, UkLimitedCompany, UnlimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class IncorporatedEntityIdentificationFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new IncorporatedEntityIdentificationFrontendConnectorImpl(appConfig, mockHttpClient)
  val apiUrl                     = s"${appConfig.incorporatedEntityIdentificationFrontendBaseUrl}/incorporated-entity-identification/api"

  "createUkCompanyJourney" should {
    "return a GRS create journey response for the given request when the http client returns a GRS create journey response for the given request" in forAll {
      (
        grsCreateJourneyResponse: GrsCreateJourneyResponse,
        incorporatedEntityType: IncorporatedEntityType,
        mode: Mode
      ) =>
        val entityType = incorporatedEntityType.entityType

        val expectedUrl: String = incorporatedEntityType.entityType match {
          case UkLimitedCompany | UnlimitedCompany => s"$apiUrl/limited-company-journey"
          case RegisteredSociety                   => s"$apiUrl/registered-society-journey"
          case e                                   => throw new IllegalArgumentException(s"$e is not a valid incorporated entity type")
        }

        val expectedIncorporatedEntityCreateJourneyRequest: IncorporatedEntityCreateJourneyRequest = {
          val serviceNameLabels = ServiceNameLabels(
            OptServiceName("Register for the Economic Crime Levy"),
            OptServiceName("Cofrestru ar gyfer yr Ardoll Troseddau Economaidd")
          )

          IncorporatedEntityCreateJourneyRequest(
            continueUrl =
              s"http://localhost:14000/register-for-economic-crime-levy/grs-continue/${mode.toString.toLowerCase}",
            businessVerificationCheck = false,
            optServiceName = Some(serviceNameLabels.en.optServiceName),
            deskProServiceId = "economic-crime-levy-registration-frontend",
            signOutUrl = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
            accessibilityUrl = "/accessibility-statement/economic-crime-levy",
            labels = serviceNameLabels
          )
        }

        when(
          mockHttpClient.POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedIncorporatedEntityCreateJourneyRequest),
            any()
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(grsCreateJourneyResponse))

        val result = await(connector.createIncorporatedEntityJourney(entityType, mode))

        result shouldBe grsCreateJourneyResponse

        verify(mockHttpClient, times(1))
          .POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedIncorporatedEntityCreateJourneyRequest),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

  "getJourneyData" should {
    "return journey data for a given journey id" in forAll {
      (incorporatedEntityJourneyData: IncorporatedEntityJourneyData, journeyId: String) =>
        val expectedUrl = s"$apiUrl/journey/$journeyId"

        when(
          mockHttpClient.GET[IncorporatedEntityJourneyData](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())
        )
          .thenReturn(Future.successful(incorporatedEntityJourneyData))

        val result = await(connector.getJourneyData(journeyId))

        result shouldBe incorporatedEntityJourneyData

        verify(mockHttpClient, times(1))
          .GET[IncorporatedEntityJourneyData](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }
}
