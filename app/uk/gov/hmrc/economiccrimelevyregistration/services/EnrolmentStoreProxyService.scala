/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, GroupEnrolmentsResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyService @Inject() (enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit
  ec: ExecutionContext
) {

  def groupHasEnrolment(groupId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    enrolmentStoreProxyConnector.getEnrolmentsForGroup(groupId).flatMap {
      case Some(groupEnrolmentsResponse: GroupEnrolmentsResponse) =>
        Future.successful(groupEnrolmentsResponse.enrolments.exists(e => e.service == EclEnrolment.ServiceName))
      case None                                                   => Future.successful(false)
    }
}
