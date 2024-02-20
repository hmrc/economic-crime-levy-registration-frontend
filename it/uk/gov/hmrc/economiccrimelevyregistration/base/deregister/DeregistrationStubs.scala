package uk.gov.hmrc.economiccrimelevyregistration.base.deregister

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockStubs
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration

trait DeregistrationStubs { self: WireMockStubs =>

  def stubGetDeregistration(deregistration: Deregistration): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/deregistration/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(deregistration.copy(internalId = testInternalId)).toString())
    )

  def stubUpsertDeregistration(deregistration: Deregistration): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/deregistration"))
        .withRequestBody(
          equalToJson(Json.toJson(deregistration).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
    )

  def stubDeleteDeregistration(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-registration/deregistration/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
