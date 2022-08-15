/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

trait WireMockConfig {
  me: BaseISpec with WireMockSupport =>

  additionalAppConfig ++=
    setWireMockPort(
      "auth",
      "economic-crime-levy-registration"
    )

  private def setWireMockPort(services: String*): Map[String, Any] =
    services.foldLeft(Map.empty[String, Any]) {
      case (map, service) => map + (s"microservice.services.$service.port" -> mockServerPort)
    }
}
