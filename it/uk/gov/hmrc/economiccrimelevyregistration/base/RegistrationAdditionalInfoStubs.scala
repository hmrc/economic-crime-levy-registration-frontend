package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationAdditionalInfo

trait RegistrationAdditionalInfoStubs { self: WireMockStubs =>

  def stubGetRegistrationAdditionalInfo(registrationAdditionalInfo: RegistrationAdditionalInfo): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/registration-additional-info/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(registrationAdditionalInfo).toString())
    )

  def stubUpsertRegistrationAdditionalInfo(registrationAdditionalInfo: RegistrationAdditionalInfo): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/registration-additional-info"))
        .withRequestBody(
          equalToJson(Json.toJson(registrationAdditionalInfo).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
    )
  def stubDeleteRegistrationAdditionalInfo(): StubMapping                                                       =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-registration/registration-additional-info/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
