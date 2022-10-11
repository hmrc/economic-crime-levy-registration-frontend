/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.IncorporatedEntityJourneyData

trait WireMockStubs {

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(200)
        .withBody(s"""
             |{
             |  "internalId": "test-id"
             |}
           """.stripMargin)
    )

  def stubGetRegistration(registration: Registration): StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-registration/registrations/test-id")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(registration).toString())
    )

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
        .withBody(s"""
           |{
           |    "journeyStartUrl": "http://localhost:9718/identify-your-incorporated-business/e9e5b979-26e8-4f33-90b0-7e5e092ed095/company-number"
           |}
         """.stripMargin)
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

  def stubGetJourneyData(journeyId: String, journeyData: IncorporatedEntityJourneyData): StubMapping =
    stub(
      get(urlEqualTo(s"/incorporated-entity-identification/api/journey/$journeyId")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(journeyData).toString())
    )
}
