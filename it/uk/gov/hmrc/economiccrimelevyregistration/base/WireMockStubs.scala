/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait WireMockStubs {

  def stubAuthorised(): StubMapping = {
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
        .withBody(
          s"""
             |{
             |  "internalId": "test-id"
             |}
           """.stripMargin)
    )
  }

  def stubGetRegistration(): StubMapping = {
    stub(
      get(urlEqualTo("/economic-crime-levy-registration/registrations/test-id")),
      aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "internalId": "test-id"
             |}
     """.stripMargin)
    )
  }
}
