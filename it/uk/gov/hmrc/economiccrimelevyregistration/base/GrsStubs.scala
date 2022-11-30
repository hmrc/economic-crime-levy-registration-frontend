package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait GrsStubs { self: WireMockStubs =>

  def stubCreateGrsJourney(url: String): StubMapping =
    stub(
      post(urlEqualTo(url))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "continueUrl" : "http://localhost:14000/register-for-the-economic-crime-levy/grs-continue",
               |  "businessVerificationCheck" : true,
               |  "optServiceName" : "Register for the Economic Crime Levy",
               |  "deskProServiceId" : "economic-crime-levy-registration-frontend",
               |  "signOutUrl" : "http://localhost:14000/register-for-the-economic-crime-levy/account/sign-out-survey",
               |  "regime" : "ECL",
               |  "accessibilityUrl" : "/accessibility-statement/register-for-the-economic-crime-levy",
               |  "labels" : {
               |    "en" : {
               |      "optServiceName" : "Register for the Economic Crime Levy"
               |    },
               |    "cy" : {
               |      "optServiceName" : "service.name"
               |    }
               |  }
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(CREATED)
        .withBody(s"""
             |{
             |    "journeyStartUrl": "test-url"
             |}
         """.stripMargin)
    )

  def stubGetGrsJourneyData[T](url: String, journeyData: T)(implicit writes: Writes[T]): StubMapping =
    stub(
      get(urlEqualTo(url)),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(journeyData).toString())
    )

}
