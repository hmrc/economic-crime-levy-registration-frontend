/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WireMockStubs extends ISpecBase with WireMockHelper with WireMockSupport with WireMockConfig {

  /*
  This is to initialise the app and server before running any tests, as they are lazy by default in org.scalatestplus.play.BaseOneServerPerSuite.
  It enables us to include behaviour tests that need to run within the `should` part of a test but before `in`.
   */
  private val _ = (app, runningServer)

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
             |{
             |  "authorise": [],
             |  "retrieve": []
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
                     |}
           """.stripMargin)
    )

}
