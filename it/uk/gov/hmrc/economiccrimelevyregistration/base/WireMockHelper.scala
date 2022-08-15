/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WireMockHelper {
  this: ISpecBase =>

  def stub(method: MappingBuilder, response: ResponseDefinitionBuilder): StubMapping =
    stubFor(method.willReturn(response))

  def stubGet(uri: String, responseBody: String): StubMapping =
    stub(get(urlEqualTo(uri)), okJson(responseBody))

  def stubPost(url: String, responseStatus: Int, responseBody: String, responseHeader: (String, String) = ("", "")): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      post(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        ))
  }

  def stubDelete(url: String, responseStatus: Int, responseBody: String, responseHeader: (String, String) = ("", "")): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      delete(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        ))
  }

  def stubPut(url: String, responseStatus: Int, responseBody: String, responseHeader: (String, String) = ("", "")): StubMapping = {
    removeStub(put(urlMatching(url)))
    stubFor(
      put(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        ))
  }
}
