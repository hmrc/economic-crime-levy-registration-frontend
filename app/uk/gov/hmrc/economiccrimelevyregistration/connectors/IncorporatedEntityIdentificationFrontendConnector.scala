/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{RegisteredSociety, UkLimitedCompany, UnlimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Mode}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IncorporatedEntityIdentificationFrontendConnector {
  def createIncorporatedEntityJourney(incorporatedEntityType: EntityType, mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse]
  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData]
}

class IncorporatedEntityIdentificationFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends IncorporatedEntityIdentificationFrontendConnector
    with BaseConnector
    with Retries {

  private val apiUrl: URL =
    url"${appConfig.incorporatedEntityIdentificationFrontendBaseUrl}/incorporated-entity-identification/api"

  def createIncorporatedEntityJourney(incorporatedEntityType: EntityType, mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    val url: URL = incorporatedEntityType match {
      case UkLimitedCompany | UnlimitedCompany => url"$apiUrl/limited-company-journey"
      case RegisteredSociety                   => url"$apiUrl/registered-society-journey"
      case _                                   => url""
    }

    retryFor[GrsCreateJourneyResponse]("Incorporated entity identification - Create journey")(retryCondition) {
      httpClient
        .post(url)
        .withBody(
          Json.toJson(
            toIncorporatedEntityCreateJourneyRequest(mode, serviceNameLabels)
          )
        )
        .executeAndDeserialise[GrsCreateJourneyResponse]
    }
  }

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData] =
    retryFor[IncorporatedEntityJourneyData]("Incorporated entity identification - Get entity journey data")(
      retryCondition
    ) {
      httpClient
        .get(url"$apiUrl/journey/$journeyId")
        .executeAndDeserialise[IncorporatedEntityJourneyData]
    }

  private def toIncorporatedEntityCreateJourneyRequest(
    mode: Mode,
    serviceNameLabels: ServiceNameLabels
  ): IncorporatedEntityCreateJourneyRequest =
    IncorporatedEntityCreateJourneyRequest(
      continueUrl = s"${appConfig.grsContinueUrl}/${mode.toString.toLowerCase}",
      businessVerificationCheck = appConfig.incorporatedEntityBvEnabled,
      optServiceName = Some(serviceNameLabels.en.optServiceName),
      deskProServiceId = appConfig.appName,
      signOutUrl = appConfig.eclSignOutUrl,
      accessibilityUrl = appConfig.accessibilityStatementPath,
      labels = serviceNameLabels
    )
}
