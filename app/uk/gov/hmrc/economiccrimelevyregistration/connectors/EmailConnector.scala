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
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.email.AmendRegistrationSubmittedEmailParameters.AmendRegistrationTemplateId
import uk.gov.hmrc.economiccrimelevyregistration.models.email.RegistrationSubmittedEmailRequest.{NormalEntityTemplateId, OtherEntityTemplateId}
import uk.gov.hmrc.economiccrimelevyregistration.models.email._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends BaseConnector
    with Retries {

  private val sendEmailUrl: URL = url"${appConfig.emailBaseUrl}/hmrc/email"

  def sendRegistrationSubmittedEmail(
    to: String,
    registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters,
    entityType: Option[EntityType]
  )(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    retryFor[Unit]("HMRC email - Send registration")(retryCondition) {
      httpClient
        .post(sendEmailUrl)
        .withBody(
          Json.toJson(toRegistrationSubmittedEmailRequest(to, registrationSubmittedEmailParameters, entityType))
        )
        .executeAndContinue
    }

  def sendAmendRegistrationSubmittedEmail(
    to: String,
    amendRegistrationSubmittedEmailParameters: AmendRegistrationSubmittedEmailParameters
  )(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    retryFor[Unit]("HMRC email - Send amend registration")(retryCondition) {
      httpClient
        .post(sendEmailUrl)
        .withBody(
          Json.toJson(toAmendRegistrationSubmittedEmailRequest(to, amendRegistrationSubmittedEmailParameters))
        )
        .executeAndContinue
    }

  def sendDeregistrationRequestedEmail(
    to: String,
    deregistrationRequestedEmailParameters: DeregistrationRequestedEmailParameters
  )(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    retryFor[Unit]("HMRC email - Send deregistration requested")(retryCondition) {
      httpClient
        .post(sendEmailUrl)
        .withBody(
          Json.toJson(toDeregistrationRequestedEmailRequest(to, deregistrationRequestedEmailParameters))
        )
        .executeAndContinue
    }

  private def toRegistrationSubmittedEmailRequest(
    to: String,
    registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters,
    entityType: Option[EntityType]
  ): RegistrationSubmittedEmailRequest =
    RegistrationSubmittedEmailRequest(
      to = Seq(to),
      templateId = templateIdByEntityType(entityType),
      parameters = registrationSubmittedEmailParameters
    )

  private def toAmendRegistrationSubmittedEmailRequest(
    to: String,
    amendRegistrationSubmittedEmailParameters: AmendRegistrationSubmittedEmailParameters
  ): AmendRegistrationSubmittedEmailRequest =
    AmendRegistrationSubmittedEmailRequest(
      to = Seq(to),
      templateId = AmendRegistrationTemplateId,
      parameters = amendRegistrationSubmittedEmailParameters
    )

  private def toDeregistrationRequestedEmailRequest(
    to: String,
    deregistrationRequestedEmailParameters: DeregistrationRequestedEmailParameters
  ): DeregistrationRequestedEmailRequest =
    DeregistrationRequestedEmailRequest(
      to = Seq(to),
      parameters = deregistrationRequestedEmailParameters
    )

  private def templateIdByEntityType(entityType: Option[EntityType]) =
    entityType match {
      case Some(value) if EntityType.isOther(value) =>
        OtherEntityTemplateId
      case _                                        =>
        NormalEntityTemplateId
    }
}
