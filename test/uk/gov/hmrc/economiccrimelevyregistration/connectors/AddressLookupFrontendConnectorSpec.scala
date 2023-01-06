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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class AddressLookupFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new AddressLookupFrontendConnectorImpl(appConfig, mockHttpClient)
  val baseUrl                    = appConfig.addressLookupFrontendUrl

  "initJourney" should {
    "return the url for the address lookup journey in the Location header" in forAll {
      (journeyUrl: String, ukMode: Boolean) =>
        val expectedUrl = s"$baseUrl/api/init"

        val expectedJourneyConfig: AlfJourneyConfig = {
          val alfLabels = AlfEnCyLabels(
            en = AlfLabels(
              appLevelLabels = AlfAppLabels(
                navTitle = "???",
                phaseBannerHtml = "???"
              ),
              countryPickerLabels = AlfCountryPickerLabels(
                submitLabel = "Save and continue"
              ),
              selectPageLabels = AlfSelectPageLabels(
                title = "Select your address",
                heading = "Select your address",
                submitLabel = "Save and continue"
              ),
              lookupPageLabels = AlfLookupPageLabels(
                title = "What address do you want to use as the contact address?",
                heading = "What address do you want to use as the contact address?",
                postcodeLabel = "UK postcode",
                submitLabel = "Save and continue"
              ),
              editPageLabels = AlfEditPageLabels(
                title = "What address do you want to use as the contact address?",
                heading = "What address do you want to use as the contact address?",
                submitLabel = "Save and continue"
              )
            ),
            cy = AlfLabels(
              appLevelLabels = AlfAppLabels(
                navTitle = "alf.labels.app.title",
                phaseBannerHtml = "alf.labels.app.banner"
              ),
              countryPickerLabels = AlfCountryPickerLabels(
                submitLabel = "alf.labels.submit"
              ),
              selectPageLabels = AlfSelectPageLabels(
                title = "alf.labels.select.title",
                heading = "alf.labels.select.heading",
                submitLabel = "alf.labels.submit"
              ),
              lookupPageLabels = AlfLookupPageLabels(
                title = "alf.labels.lookup.title",
                heading = "alf.labels.lookup.heading",
                postcodeLabel = "alf.labels.lookup.postcode",
                submitLabel = "alf.labels.submit"
              ),
              editPageLabels = AlfEditPageLabels(
                title = "alf.labels.edit.title",
                heading = "alf.labels.edit.heading",
                submitLabel = "alf.labels.submit"
              )
            )
          )

          AlfJourneyConfig(
            options = AlfOptions(
              continueUrl = "http://localhost:14000/register-for-the-economic-crime-levy/address-lookup-continue",
              homeNavHref = "http://www.hmrc.gov.uk/",
              signOutHref = "http://localhost:14000/register-for-the-economic-crime-levy/account/sign-out-survey",
              accessibilityFooterUrl = "",
              deskProServiceName = "",
              ukMode = ukMode
            ),
            labels = alfLabels
          )
        }

        when(
          mockHttpClient.POST[AlfJourneyConfig, String](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedJourneyConfig),
            any()
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(journeyUrl))

        val result = await(connector.initJourney(ukMode))

        result shouldBe journeyUrl

        verify(mockHttpClient, times(1))
          .POST[AlfJourneyConfig, String](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedJourneyConfig),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

  "getAddress" should {
    "return the address identified by the given journey id" in {
      pending
    }
  }
}
