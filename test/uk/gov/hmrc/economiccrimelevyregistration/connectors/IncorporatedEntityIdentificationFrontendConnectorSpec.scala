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

import scala.concurrent.Future

class IncorporatedEntityIdentificationFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new IncorporatedEntityIdentificationFrontendConnector(appConfig, mockHttpClient)
  val limitedCompanyJourneyUrl   =
    s"${appConfig.incorporatedEntityIdentificationApiUrl}/incorporated-entity-identification/api/limited-company-journey"

  override def afterEach(): Unit =
    reset(mockHttpClient)

  "createLimitedCompanyJourney" should {
    val expectedUrl = limitedCompanyJourneyUrl

    val expectedIncorporatedEntityCreateJourneyRequest: IncorporatedEntityCreateJourneyRequest = {
      val serviceNameLabels = ServiceNameLabels(
        En("Register for Economic Crime Levy"),
        Cy("service.name")
      )

      IncorporatedEntityCreateJourneyRequest(
        continueUrl = "http://localhost:14000/register-for-economic-crime-levy/grs-continue",
        optServiceName = Some(serviceNameLabels.en.optServiceName),
        deskProServiceId = "economic-crime-levy-registration-frontend",
        signOutUrl = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
        accessibilityUrl = "/accessibility-statement/register-for-economic-crime-levy",
        labels = serviceNameLabels
      )
    }

    val emptyGrsCreateJourneyResponse: GrsCreateJourneyResponse = GrsCreateJourneyResponse("")

    "return a GRS create journey response for the given request when the http client returns a GRS create journey response for the given request" in {
      when(
        mockHttpClient.POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
          ArgumentMatchers.eq(expectedUrl),
          ArgumentMatchers.eq(expectedIncorporatedEntityCreateJourneyRequest),
          any()
        )(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(emptyGrsCreateJourneyResponse))

      val result = await(connector.createLimitedCompanyJourney())

      result shouldBe emptyGrsCreateJourneyResponse

      verify(mockHttpClient, times(1))
        .POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
          ArgumentMatchers.eq(expectedUrl),
          ArgumentMatchers.eq(expectedIncorporatedEntityCreateJourneyRequest),
          any()
        )(any(), any(), any(), any())
    }
  }
}
