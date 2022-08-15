/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: Suite =>

  val mockServerHost: String = "localhost"
  val mockServerPort: Int    = 9999
  val mockServerUrl = s"http://$mockServerHost:$mockServerPort"

  val mockServer = new WireMockServer(wireMockConfig().port(mockServerPort))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor("localhost", mockServerPort)
    mockServer.start()
  }

  override protected def beforeEach(): Unit =
    super.beforeEach()

  override protected def afterEach(): Unit = {
    WireMock.reset()
    super.afterEach()
  }

  override protected def afterAll(): Unit = {
    mockServer.stop()
    super.afterAll()
  }
}
