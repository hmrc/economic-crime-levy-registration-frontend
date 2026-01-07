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
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{RegisteredSociety, UkLimitedCompany, UnlimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class IncorporatedEntityIdentificationFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          =
    new IncorporatedEntityIdentificationFrontendConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val apiUrl                             = s"${appConfig.incorporatedEntityIdentificationFrontendBaseUrl}/incorporated-entity-identification/api"

  "createUkCompanyJourney" should {
    "return a GRS create journey response for the given request when the http client returns a GRS create journey response for the given request" in forAll {
      (
        grsCreateJourneyResponse: GrsCreateJourneyResponse,
        incorporatedEntityType: IncorporatedEntityType,
        mode: Mode
      ) =>
        val entityType = incorporatedEntityType.entityType
        val response   = HttpResponse(OK, Json.toJson(grsCreateJourneyResponse).toString())

        val expectedUrl = incorporatedEntityType.entityType match {
          case UkLimitedCompany | UnlimitedCompany => url"$apiUrl/limited-company-journey"
          case RegisteredSociety                   => url"$apiUrl/registered-society-journey"
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
        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder
            .withBody(ArgumentMatchers.eq(Json.toJson(expectedIncorporatedEntityCreateJourneyRequest)))(
              any(),
              any(),
              any()
            )
        )
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = await(connector.createIncorporatedEntityJourney(entityType, mode))

        result shouldBe grsCreateJourneyResponse

    }
  }

  "getJourneyData" should {
    "return journey data for a given journey id" in forAll {
      (incorporatedEntityJourneyData: IncorporatedEntityJourneyData, journeyId: String) =>
        val expectedUrl = url"$apiUrl/journey/$journeyId"
        val response    = HttpResponse(OK, Json.toJson(incorporatedEntityJourneyData).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getJourneyData(journeyId))

        result shouldBe incorporatedEntityJourneyData
    }
  }
}
