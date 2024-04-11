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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors.stubs

import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.KeyValue
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, Enrolment, GroupEnrolmentsResponse}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.Future

class StubEnrolmentStoreProxyConnector @Inject() (appConfig: AppConfig) extends EnrolmentStoreProxyConnector {
  override def getEnrolmentsForGroup(groupId: String)(implicit
    hc: HeaderCarrier
  ): Future[GroupEnrolmentsResponse] =
    if (appConfig.enrolmentStoreProxyStubReturnsEclReference) {
      val groupEnrolmentsWithEcl = GroupEnrolmentsResponse(
        Seq(
          Enrolment(
            service = EclEnrolment.serviceName,
            identifiers = Seq(KeyValue(key = EclEnrolment.identifierKey, value = "XMECL0000000001"))
          )
        )
      )

      Future.successful(groupEnrolmentsWithEcl)
    } else {
      Future.failed(UpstreamErrorResponse.apply("", NOT_FOUND))
    }

}
