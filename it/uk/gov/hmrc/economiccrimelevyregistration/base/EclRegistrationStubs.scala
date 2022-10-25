package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

trait EclRegistrationStubs {

  def stubGetRegistration(registration: Registration): StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-registration/registrations/test-internal-id")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(registration).toString())
    )

  def stubUpsertRegistration(registration: Registration): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/registrations"))
        .withRequestBody(
          equalToJson(Json.toJson(registration).toString(), true, true)
        ),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(registration).toString())
    )

}
