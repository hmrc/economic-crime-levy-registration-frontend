package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment

trait EnrolmentStoreProxyStubs {

  def stubNoGroupEnrolment(): StubMapping =
    stub(
      get(urlEqualTo("/enrolment-store/groups/test-group-id/enrolments")),
      aResponse()
        .withStatus(204)
    )

  def stubWithGroupEclEnrolment(): StubMapping =
    stub(
      get(urlEqualTo("/enrolment-store/groups/test-group-id/enrolments")),
      aResponse()
        .withStatus(200)
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
            |                 "key": "EtmpRegistrationNumber",
            |                 "value": "X00000123456789"
            |              }
            |           ]
            |        }
            |    ]
            |}
            |""".stripMargin)
    )

}
