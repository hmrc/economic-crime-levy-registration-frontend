package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.{SessionData, SessionKeys}

trait SessionDataStubs { self: WireMockStubs =>

  def stubGetSession(sessionData: SessionData): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/session/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(sessionData).toString())
    )

  def stubUpsertSession(sessionData: SessionData): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/session"))
        .withRequestBody(
          equalToJson(Json.toJson(sessionData).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(sessionData).toString())
    )

  def stubSessionForStoreUrl(call: Call) = {
    stubGetSession(SessionData(testInternalId, Map()))
    stubUpsertSession(SessionData(testInternalId, Map(SessionKeys.UrlToReturnTo -> call.url)))
  }

  def stubDeleteSession(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-registration/session/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
