package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models._

trait SessionDataStubs { self: WireMockStubs =>

  def stubGetSession(sessionData: SessionData): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/session/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(sessionData).toString())
    )

  def stubUpsertSession(): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/session")),
      aResponse()
        .withStatus(OK)
    )

  def stubSessionForStoreUrl(): StubMapping = {
    stubGetSession(SessionData(testInternalId, Map()))
    stubUpsertSession()
  }

  def stubDeleteSession(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-registration/session/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
