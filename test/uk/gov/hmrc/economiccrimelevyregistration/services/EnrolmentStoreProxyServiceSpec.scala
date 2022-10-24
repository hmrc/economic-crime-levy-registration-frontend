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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.{GroupEnrolmentsResponseWithEcl, GroupEnrolmentsResponseWithoutEcl}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector

import scala.concurrent.Future

class EnrolmentStoreProxyServiceSpec extends SpecBase {
  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]
  val service                                                        = new EnrolmentStoreProxyService(mockEnrolmentStoreProxyConnector)

  "groupHasEnrolment" should {
    "return true when the list of group enrolments contains the ECL enrolment" in forAll {
      (groupId: String, groupEnrolmentsWithEcl: GroupEnrolmentsResponseWithEcl) =>
        when(mockEnrolmentStoreProxyConnector.getEnrolmentsForGroup(any())(any()))
          .thenReturn(Future.successful(Some(groupEnrolmentsWithEcl.groupEnrolmentsResponse)))

        val result = await(service.groupHasEnrolment(groupId))
        result shouldBe true
    }

    "return false when the list of group enrolments does not contain the ECL enrolment" in forAll {
      (groupId: String, groupEnrolmentsWithoutEcl: GroupEnrolmentsResponseWithoutEcl) =>
        when(mockEnrolmentStoreProxyConnector.getEnrolmentsForGroup(any())(any()))
          .thenReturn(Future.successful(Some(groupEnrolmentsWithoutEcl.groupEnrolmentsResponse)))

        val result = await(service.groupHasEnrolment(groupId))
        result shouldBe false
    }

    "return false when there are no group enrolments returned" in forAll { groupId: String =>
      when(mockEnrolmentStoreProxyConnector.getEnrolmentsForGroup(any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.groupHasEnrolment(groupId))
      result shouldBe false
    }
  }

}
