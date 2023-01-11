package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.LOCATION
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.{AlfAddressData, AlfJourneyConfig}

trait AlfStubs { self: WireMockStubs =>

  def stubInitAlfJourney(journeyConfig: AlfJourneyConfig): StubMapping =
    stub(
      post(urlEqualTo("/api/init"))
        .withRequestBody(equalToJson(Json.toJson(journeyConfig).toString())),
      aResponse()
        .withStatus(CREATED)
        .withHeader(LOCATION, "test-url")
    )

  def stubGetAlfAddressData(journeyId: String, alfAddressData: AlfAddressData): StubMapping =
    stub(
      get(urlEqualTo(s"/api/confirmed?id=$journeyId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(alfAddressData).toString())
    )
}
