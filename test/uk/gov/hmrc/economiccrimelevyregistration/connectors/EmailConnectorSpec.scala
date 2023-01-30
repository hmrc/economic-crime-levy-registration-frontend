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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{RegistrationSubmittedEmailParameters, RegistrationSubmittedEmailRequest}
import uk.gov.hmrc.http.{HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class EmailConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EmailConnector(appConfig, mockHttpClient)
  val sendEmailUrl: String       = s"${appConfig.emailBaseUrl}/hmrc/email"

  "sendRegistrationSubmittedEmail" should {
    val expectedUrl = sendEmailUrl

    "return unit when the http client returns a successful http response" in forAll {
      (to: String, registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters) =>
        val response = HttpResponse(ACCEPTED, "")

        when(
          mockHttpClient
            .POST[RegistrationSubmittedEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](
              ArgumentMatchers.eq(expectedUrl),
              ArgumentMatchers.eq(
                RegistrationSubmittedEmailRequest(
                  to,
                  templateId = "ecl_registration_submitted",
                  registrationSubmittedEmailParameters,
                  force = false,
                  None
                )
              ),
              any()
            )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(Right(response)))

        val result: Unit = await(connector.sendRegistrationSubmittedEmail(to, registrationSubmittedEmailParameters))

        result shouldBe ()

        verify(mockHttpClient, times(1))
          .POST[RegistrationSubmittedEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }

    "throw an exception when the http client returns an upstream error response" in forAll {
      (to: String, registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters) =>
        val response = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(
          mockHttpClient
            .POST[RegistrationSubmittedEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](
              ArgumentMatchers.eq(expectedUrl),
              ArgumentMatchers.eq(
                RegistrationSubmittedEmailRequest(
                  to,
                  templateId = "ecl_registration_submitted",
                  registrationSubmittedEmailParameters,
                  force = false,
                  None
                )
              ),
              any()
            )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(Left(response)))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.sendRegistrationSubmittedEmail(to, registrationSubmittedEmailParameters))
        }

        result.getMessage shouldBe "Internal server error"

        verify(mockHttpClient, times(1))
          .POST[RegistrationSubmittedEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(
              RegistrationSubmittedEmailRequest(
                to,
                templateId = "ecl_registration_submitted",
                registrationSubmittedEmailParameters,
                force = false,
                None
              )
            ),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }
}
