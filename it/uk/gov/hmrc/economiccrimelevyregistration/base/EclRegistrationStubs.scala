package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

trait EclRegistrationStubs { self: WireMockStubs =>

  def stubGetRegistration(registration: Registration): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/registrations/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(registration).toString())
    )

  def stubUpsertRegistration(registration: Registration): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/registrations"))
        .withRequestBody(
          equalToJson(Json.toJson(registration).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(registration).toString())
    )

  def stubGetSubscriptionStatus(businessPartnerId: String): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/subscription-status/$businessPartnerId")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "NotSubscribed"
             |}
           """.stripMargin)
    )
}
