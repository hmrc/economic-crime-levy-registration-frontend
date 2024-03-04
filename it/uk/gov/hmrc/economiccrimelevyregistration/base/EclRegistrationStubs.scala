package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CreateEclSubscriptionResponse, EclSubscriptionStatus, GetSubscriptionResponse, Registration, RegistrationAdditionalInfo}

import java.time.Instant

trait EclRegistrationStubs { self: WireMockStubs =>

  def stubGetRegistrationWithEmptyAdditionalInfo(registration: Registration): StubMapping = {
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/registrations/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(registration).toString())
    )

    stubGetRegistrationAdditionalInfo(
      new RegistrationAdditionalInfo(registration.internalId, None, None, None, None, None)
    )
  }

  def stubGetRegistration(registration: Registration): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/registrations/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(registration).toString())
    )

  def stubGetSubscription(getSubscriptionResponse: GetSubscriptionResponse): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/subscription/$testEclRegistrationReference")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(getSubscriptionResponse).toString())
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

  def stubUpsertRegistrationWithoutRequestMatching(registration: Registration): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-registration/registrations")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(registration).toString())
    )

  def stubGetSubscriptionStatus(businessPartnerId: String, eclSubscriptionStatus: EclSubscriptionStatus): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/subscription-status/SAFE/$businessPartnerId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(eclSubscriptionStatus).toString())
    )

  def stubGetRegistrationValidationErrors(valid: Boolean, errors: DataValidationError): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/registrations/$testInternalId/validation-errors")),
      if (valid) {
        aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(None).toString())
      } else {
        aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(errors.message).toString())
      }
    )

  def stubSubmitRegistration(eclReference: String): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy-registration/submit-registration/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(CreateEclSubscriptionResponse(Instant.now, eclReference)).toString())
    )

  def stubDeleteRegistration(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-registration/registrations/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
