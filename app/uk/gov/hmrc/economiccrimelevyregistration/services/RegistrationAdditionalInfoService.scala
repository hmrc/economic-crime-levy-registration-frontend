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

package uk.gov.hmrc.economiccrimelevyregistration.services

import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, RegistrationAdditionalInfoConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RegistrationStartedEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationAdditionalInfoService @Inject() (
  registrationAdditionalInfoConnector: RegistrationAdditionalInfoConnector
)(implicit
  ec: ExecutionContext
) {
  def get(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Option[RegistrationAdditionalInfo]] =
    registrationAdditionalInfoConnector.get(internalId)

  def createOrUpdate(
    internalId: String,
    eclReference: Option[String]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    registrationAdditionalInfoConnector.upsert(RegistrationAdditionalInfo(internalId, None, eclReference))

  def createOrUpdate(
    info: RegistrationAdditionalInfo
  )(implicit hc: HeaderCarrier): Future[Unit] =
    registrationAdditionalInfoConnector.upsert(info)

  def delete(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    registrationAdditionalInfoConnector.delete(internalId)
}
