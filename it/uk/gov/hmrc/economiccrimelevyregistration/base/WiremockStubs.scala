/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WiremockStubs extends ISpecBase with WireMockHelper with WireMockSupport with WireMockConfig {

  /*
  This is to initialise the app before running any tests, as it is lazy by default in org.scalatestplus.play.BaseOneAppPerSuite.
  It enables us to include behaviour tests that need to call routes within the `should` part of a test but before `in`.
  */
  private val _ = app

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
