package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

trait GrsStubs {

  def stubCreateLimitedCompanyJourney(): StubMapping =
    stub(
      post(urlEqualTo("/incorporated-entity-identification/api/limited-company-journey"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "continueUrl" : "http://localhost:14000/register-for-economic-crime-levy/grs-continue",
               |  "businessVerificationCheck" : true,
               |  "optServiceName" : "Register for Economic Crime Levy",
               |  "deskProServiceId" : "economic-crime-levy-registration-frontend",
               |  "signOutUrl" : "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
               |  "regime" : "ECL",
               |  "accessibilityUrl" : "/accessibility-statement/register-for-economic-crime-levy",
               |  "labels" : {
               |    "en" : {
               |      "optServiceName" : "Register for Economic Crime Levy"
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
        .withStatus(201)
        .withBody(
          s"""
             |{
             |    "journeyStartUrl": "test-url"
             |}
         """.stripMargin)
    )

  def stubCreateSoleTraderJourney(): StubMapping =
    stub(
      post(urlEqualTo("/sole-trader-identification/api/sole-trader-journey"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "continueUrl" : "http://localhost:14000/register-for-economic-crime-levy/grs-continue",
               |  "businessVerificationCheck" : true,
               |  "optServiceName" : "Register for Economic Crime Levy",
               |  "deskProServiceId" : "economic-crime-levy-registration-frontend",
               |  "signOutUrl" : "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
               |  "regime" : "ECL",
               |  "accessibilityUrl" : "/accessibility-statement/register-for-economic-crime-levy",
               |  "labels" : {
               |    "en" : {
               |      "optServiceName" : "Register for Economic Crime Levy"
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
        .withStatus(201)
        .withBody(
          s"""
             |{
             |    "journeyStartUrl": "test-url"
             |}
     """.stripMargin)
    )

  def stubCreatePartnershipJourney(url: String): StubMapping =
    stub(
      post(urlEqualTo(s"/partnership-identification/api/$url"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "continueUrl" : "http://localhost:14000/register-for-economic-crime-levy/grs-continue",
               |  "optServiceName" : "Register for Economic Crime Levy",
               |  "deskProServiceId" : "economic-crime-levy-registration-frontend",
               |  "signOutUrl" : "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
               |  "regime" : "ECL",
               |  "accessibilityUrl" : "/accessibility-statement/register-for-economic-crime-levy",
               |  "labels" : {
               |    "en" : {
               |      "optServiceName" : "Register for Economic Crime Levy"
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
        .withStatus(201)
        .withBody(
          s"""
             |{
             |    "journeyStartUrl": "test-url"
             |}
       """.stripMargin)
    )

  def stubGetIncorporatedEntityJourneyData(journeyId: String, journeyData: IncorporatedEntityJourneyData): StubMapping =
    stub(
      get(urlEqualTo(s"/incorporated-entity-identification/api/journey/$journeyId")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(journeyData).toString())
    )

  def stubGetSoleTraderEntityJourneyData(journeyId: String, journeyData: SoleTraderEntityJourneyData): StubMapping =
    stub(
      get(urlEqualTo(s"/sole-trader-identification/api/journey/$journeyId")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(journeyData).toString())
    )

  def stubGetPartnershipEntityJourneyData(journeyId: String, journeyData: PartnershipEntityJourneyData): StubMapping =
    stub(
      get(urlEqualTo(s"/partnership-identification/api/journey/$journeyId")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(journeyData).toString())
    )

}
