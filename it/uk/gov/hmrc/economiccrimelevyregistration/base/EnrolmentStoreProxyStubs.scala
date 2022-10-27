package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment

trait EnrolmentStoreProxyStubs { self: WireMockStubs =>

  def stubNoGroupEnrolment(): StubMapping =
    stub(
      get(urlEqualTo(s"/enrolment-store/groups/$testGroupId/enrolments")),
      aResponse()
        .withStatus(NO_CONTENT)
    )

  def stubWithGroupEclEnrolment(): StubMapping =
    stub(
      get(urlEqualTo(s"/enrolment-store/groups/$testGroupId/enrolments")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
            |{
            |    "startRecord": 1,
            |    "totalRecords": 1,
            |    "enrolments": [
            |        {
            |           "service": "${EclEnrolment.ServiceName}",
            |           "state": "Activated",
            |           "friendlyName": "ECL Enrolment",
            |           "enrolmentDate": "2023-10-05T14:48:00.000Z",
            |           "failedActivationCount": 0,
            |           "activationDate": "2023-10-13T17:36:00.000Z",
            |           "identifiers": [
            |              {
            |                 "key": "${EclEnrolment.IdentifierKey}",
            |                 "value": "$testEclRegistrationNumber"
            |              }
            |           ]
            |        }
            |    ]
            |}
            |""".stripMargin)
    )

}
