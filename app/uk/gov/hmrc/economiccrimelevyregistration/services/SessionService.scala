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

import play.api.mvc.Session
import uk.gov.hmrc.economiccrimelevyregistration.connectors.SessionRetrievalConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionData
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SessionService @Inject() (sessionRetrievalConnector: SessionRetrievalConnector)(implicit ec: ExecutionContext) {

  def get(session: Session, internalId: String, key: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] = {
    val result = Try {
      session(key)
    } match {
      case Success(value) => Some(value)
      case Failure(_)     => None
    }
    if (result.isEmpty) {
      sessionRetrievalConnector
        .get(internalId)
        .map(sessionDataOption => sessionDataOption.flatMap(sessionData => sessionData.values.get(key)))
    } else {
      Future.successful(result)
    }
  }

  def update(sessionData: SessionData)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    sessionRetrievalConnector
      .upsert(sessionData)

  def delete(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    sessionRetrievalConnector.delete(internalId)

}
