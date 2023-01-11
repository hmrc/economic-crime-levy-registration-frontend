package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, Registration}

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

  def stubGetSubscriptionStatus(businessPartnerId: String, eclSubscriptionStatus: EclSubscriptionStatus): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/subscription-status/$businessPartnerId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(eclSubscriptionStatus).toString())
    )

  def stubValidateRegistration(valid: Boolean): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy-registration/registrations/validate/$testInternalId")),
      if (valid) {
        aResponse()
          .withStatus(NO_CONTENT)
      } else {
        aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(DataValidationErrors(Seq("Data is not valid"))).toString())
      }
    )

}
