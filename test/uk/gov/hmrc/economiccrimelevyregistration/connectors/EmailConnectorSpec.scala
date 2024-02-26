/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EclAddress
import uk.gov.hmrc.economiccrimelevyregistration.models.email.RegistrationSubmittedEmailRequest.NormalEntityTemplateId
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, RegistrationSubmittedEmailParameters, RegistrationSubmittedEmailRequest}
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class EmailConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder           = mock[RequestBuilder]
  val connector                    = new EmailConnector(appConfig, mockHttpClient, config, actorSystem)
  val sendEmailUrl                 = url"${appConfig.emailBaseUrl}/hmrc/email"

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "sendRegistrationSubmittedEmail" should {
    "return unit when the http client returns a successful http response" in forAll {
      (to: String, registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters) =>
        beforeEach()
        val body =
          RegistrationSubmittedEmailRequest(Seq(to), NormalEntityTemplateId, registrationSubmittedEmailParameters)

        val response = HttpResponse(ACCEPTED, "")

        when(mockHttpClient.post(ArgumentMatchers.eq(sendEmailUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(body)))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = await(connector.sendRegistrationSubmittedEmail(to, registrationSubmittedEmailParameters, None))

        result shouldBe ()
    }

    "throw an exception when the http client returns an upstream error response" in forAll {
      (to: String, registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters) =>
        beforeEach()
        val body     =
          RegistrationSubmittedEmailRequest(Seq(to), NormalEntityTemplateId, registrationSubmittedEmailParameters)
        val response = HttpResponse(INTERNAL_SERVER_ERROR, "Internal server error")

        when(mockHttpClient.post(ArgumentMatchers.eq(sendEmailUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(body)))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.sendRegistrationSubmittedEmail(to, registrationSubmittedEmailParameters, None))
        }

        result.getMessage shouldBe "Internal server error"

    }

  }
  "sendAmendRegistrationSubmittedEmail" should {
    "return unit when the http client returns a successful http response" in forAll {
      (name: String, eclAddress: EclAddress, containsAddress: Option[Boolean]) =>
        beforeEach()

        val date            = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages)
        val emailParameters = AmendRegistrationSubmittedEmailParameters(
          name,
          date,
          eclAddress.addressLine1,
          eclAddress.addressLine2,
          eclAddress.addressLine3,
          eclAddress.addressLine4,
          containsAddress
        )

        val response = HttpResponse(ACCEPTED, "")

        when(mockHttpClient.post(ArgumentMatchers.eq(sendEmailUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result: Unit = await(connector.sendAmendRegistrationSubmittedEmail(name, emailParameters))

        result shouldBe ()
    }

    "return an internal server error when the http client returns an upstream error response" in forAll {
      (name: String, email: String, eclAddress: EclAddress, containsAddress: Option[Boolean]) =>
        beforeEach()
        val date            = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages)
        val emailParameters = AmendRegistrationSubmittedEmailParameters(
          name,
          date,
          eclAddress.addressLine1,
          eclAddress.addressLine2,
          eclAddress.addressLine3,
          eclAddress.addressLine4,
          containsAddress
        )
        val response        = HttpResponse(INTERNAL_SERVER_ERROR, "Internal server error")

        when(mockHttpClient.post(ArgumentMatchers.eq(sendEmailUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.sendAmendRegistrationSubmittedEmail(email, emailParameters))
        }

        result.getMessage shouldBe "Internal server error"

    }
  }
}
