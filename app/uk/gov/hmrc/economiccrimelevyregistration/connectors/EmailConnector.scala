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

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models.email.AmendRegistrationSubmittedEmailParameters.AmendRegistrationTemplateId
import uk.gov.hmrc.economiccrimelevyregistration.models.email.RegistrationSubmittedEmailRequest.{NormalEntityTemplateId, OtherEntityTemplateId}
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, AmendRegistrationSubmittedEmailRequest, RegistrationSubmittedEmailParameters, RegistrationSubmittedEmailRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) {

  private val sendEmailUrl: String = s"${appConfig.emailBaseUrl}/hmrc/email"

  def sendRegistrationSubmittedEmail(
    to: String,
    registrationSubmittedEmailParameters: RegistrationSubmittedEmailParameters,
    entityType: Option[EntityType]
  )(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    httpClient
      .POST[RegistrationSubmittedEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](
        sendEmailUrl,
        RegistrationSubmittedEmailRequest(
          to = Seq(to),
          templateId = entityType match {
            case Some(Other) => OtherEntityTemplateId
            case _           => NormalEntityTemplateId
          },
          parameters = registrationSubmittedEmailParameters
        )
      )
      .map {
        case Left(e)  => throw e
        case Right(_) => ()
      }

  def sendAmendRegistrationSubmittedEmail(
    to: String,
    amendRegistrationSubmittedEmailParameters: AmendRegistrationSubmittedEmailParameters
  )(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    httpClient
      .POST[AmendRegistrationSubmittedEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](
        sendEmailUrl,
        AmendRegistrationSubmittedEmailRequest(
          to = Seq(to),
          templateId = AmendRegistrationTemplateId,
          parameters = amendRegistrationSubmittedEmailParameters
        )
      )
      .map {
        case Left(e)  => throw e
        case Right(_) => ()
      }
}
