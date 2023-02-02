package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.email.RegistrationSubmittedEmailRequest

trait EmailStubs { self: WireMockStubs =>

  def stubSendRegistrationSubmittedEmail(
    registrationSubmittedEmailRequest: RegistrationSubmittedEmailRequest
  ): StubMapping =
    stub(
      post(urlEqualTo("/hmrc/email"))
        .withRequestBody(
          equalToJson(
            Json.toJson(registrationSubmittedEmailRequest).toString()
          )
        ),
      aResponse()
        .withStatus(ACCEPTED)
    )

}
