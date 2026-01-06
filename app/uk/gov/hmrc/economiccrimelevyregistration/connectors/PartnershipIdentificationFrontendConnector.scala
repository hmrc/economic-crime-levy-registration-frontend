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

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}
import play.api.libs.ws.writeableOf_JsValue

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait PartnershipIdentificationFrontendConnector {
  def createPartnershipJourney(
    partnershipType: EntityType,
    mode: Mode
  )(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse]

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[PartnershipEntityJourneyData]
}

class PartnershipIdentificationFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends PartnershipIdentificationFrontendConnector
    with BaseConnector
    with Retries {

  private val apiUrl: URL =
    url"${appConfig.partnershipEntityIdentificationFrontendBaseUrl}/partnership-identification/api"

  def createPartnershipJourney(
    partnershipType: EntityType,
    mode: Mode
  )(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    val url: URL = partnershipType match {
      case GeneralPartnership          => url"$apiUrl/general-partnership-journey"
      case ScottishPartnership         => url"$apiUrl/scottish-partnership-journey"
      case LimitedPartnership          => url"$apiUrl/limited-partnership-journey"
      case ScottishLimitedPartnership  => url"$apiUrl/scottish-limited-partnership-journey"
      case LimitedLiabilityPartnership => url"$apiUrl/limited-liability-partnership-journey"
      case entityType: EntityType      =>
        throw new IllegalArgumentException(s"Invalid entity type for Partnership: $entityType")
    }

    retryFor[GrsCreateJourneyResponse]("Partnership identification - Create journey")(retryCondition) {

      httpClient
        .post(url)
        .withBody(Json.toJson(toPartnershipEntityCreateJourneyRequest(mode, serviceNameLabels)))
        .executeAndDeserialise[GrsCreateJourneyResponse]
    }
  }

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[PartnershipEntityJourneyData] =
    retryFor[PartnershipEntityJourneyData]("Partnership identification - Get journey data")(retryCondition) {
      httpClient
        .get(url"$apiUrl/journey/$journeyId")
        .executeAndDeserialise[PartnershipEntityJourneyData]
    }

  private def toPartnershipEntityCreateJourneyRequest(
    mode: Mode,
    serviceNameLabels: ServiceNameLabels
  ): PartnershipEntityCreateJourneyRequest =
    PartnershipEntityCreateJourneyRequest(
      continueUrl = s"${appConfig.grsContinueUrl}/${mode.toString.toLowerCase}",
      businessVerificationCheck = appConfig.partnershipBvEnabled,
      optServiceName = Some(serviceNameLabels.en.optServiceName),
      deskProServiceId = appConfig.appName,
      signOutUrl = appConfig.eclSignOutUrl,
      accessibilityUrl = appConfig.accessibilityStatementPath,
      labels = serviceNameLabels
    )
}
