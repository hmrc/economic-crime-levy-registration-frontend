package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait AuthStubs {

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId", "allEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "internalId": "test-id",
             |  "allEnrolments": []
             |}
           """.stripMargin)
    )

  def stubAuthorisedWithEclEnrolment(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId", "allEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "internalId": "test-id",
             |  "allEnrolments": [{
             |    "key":"HMRC-ECL-ORG",
             |    "identifiers": [{ "key":"EtmpRegistrationNumber", "value": "X00000123456789" }],
             |    "state": "activated"
             |  }]
             |}
           """.stripMargin)
    )


}
