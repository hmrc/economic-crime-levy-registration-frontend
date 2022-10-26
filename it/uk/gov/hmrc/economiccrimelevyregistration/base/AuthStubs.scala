package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment

trait AuthStubs { self: WireMockStubs =>

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId", "allEnrolments", "groupIdentifier", "affinityGroup", "credentialRole" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "$testInternalId",
             |  "groupIdentifier": "$testGroupId",
             |  "affinityGroup": "Organisation",
             |  "credentialRole": "User",
             |  "allEnrolments": []
             |}
           """.stripMargin)
    )

  def stubAuthorisedWithAgentAffinityGroup(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId", "allEnrolments", "groupIdentifier", "affinityGroup", "credentialRole" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(
          s"""
             |{
             |  "internalId": "$testInternalId",
             |  "groupIdentifier": "$testGroupId",
             |  "affinityGroup": "Agent",
             |  "credentialRole": "User",
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
               |  "retrieve": [ "internalId", "allEnrolments", "groupIdentifier", "affinityGroup", "credentialRole" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "$testInternalId",
             |  "groupIdentifier": "$testGroupId",
             |  "affinityGroup": "Organisation",
             |  "credentialRole": "User",
             |  "allEnrolments": [{
             |    "key":"${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationNumber" }],
             |    "state": "activated"
             |  }]
             |}
           """.stripMargin)
    )

  def stubAuthorisedWithAssistantCredentialRole(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId", "allEnrolments", "groupIdentifier", "affinityGroup", "credentialRole" ]
               |}
         """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(
          s"""
             |{
             |  "internalId": "$testInternalId",
             |  "groupIdentifier": "$testGroupId",
             |  "affinityGroup": "Organisation",
             |  "credentialRole": "Assistant",
             |  "allEnrolments": [{
             |    "key":"${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationNumber" }],
             |    "state": "activated"
             |  }]
             |}
         """.stripMargin)
    )

}
