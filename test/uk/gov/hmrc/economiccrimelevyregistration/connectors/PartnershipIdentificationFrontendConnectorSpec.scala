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
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.PartnershipType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.Future

class PartnershipIdentificationFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new PartnershipIdentificationFrontendConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val apiUrl                             = s"${appConfig.partnershipEntityIdentificationFrontendBaseUrl}/partnership-identification/api"

  "createPartnershipJourney" should {
    "return a GRS create journey response for the given request when the http client returns a GRS create journey response for the given request" in forAll {
      (grsCreateJourneyResponse: GrsCreateJourneyResponse, partnershipType: PartnershipType, mode: Mode) =>
        val entityType = partnershipType.entityType
        val response   = HttpResponse(OK, Json.toJson(grsCreateJourneyResponse), Map.empty)

        val expectedUrl = entityType match {
          case GeneralPartnership          => url"$apiUrl/general-partnership-journey"
          case ScottishPartnership         => url"$apiUrl/scottish-partnership-journey"
          case LimitedPartnership          => url"$apiUrl/limited-partnership-journey"
          case ScottishLimitedPartnership  => url"$apiUrl/scottish-limited-partnership-journey"
          case LimitedLiabilityPartnership => url"$apiUrl/limited-liability-partnership-journey"
        }

        val expectedPartnershipEntityCreateJourneyRequest: PartnershipEntityCreateJourneyRequest = {
          val serviceNameLabels = ServiceNameLabels(
            OptServiceName("Register for the Economic Crime Levy"),
            OptServiceName("Cofrestru ar gyfer yr Ardoll Troseddau Economaidd")
          )

          PartnershipEntityCreateJourneyRequest(
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

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedPartnershipEntityCreateJourneyRequest)))(
            any(),
            any(),
            any()
          )
        )
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.createPartnershipJourney(entityType, mode))

        result shouldBe grsCreateJourneyResponse

    }
  }

  "getJourneyData" should {
    "return journey data for a given journey id" in forAll {
      (partnershipEntityJourneyData: PartnershipEntityJourneyData, journeyId: String) =>
        val expectedUrl = url"$apiUrl/journey/$journeyId"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.toJson(partnershipEntityJourneyData).toString())))

        val result = await(connector.getJourneyData(journeyId))

        result shouldBe partnershipEntityJourneyData
    }
  }
}
