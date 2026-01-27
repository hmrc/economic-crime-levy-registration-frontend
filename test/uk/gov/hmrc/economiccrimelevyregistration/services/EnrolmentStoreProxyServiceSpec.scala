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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.economiccrimelevyregistration.{GroupEnrolmentsResponseWithEcl, GroupEnrolmentsResponseWithoutEcl}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.EnrolmentStoreProxyError
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.GroupEnrolmentsResponse
import org.scalacheck.Arbitrary.arbitrary

import scala.concurrent.Future

class EnrolmentStoreProxyServiceSpec extends SpecBase {
  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]
  val service                                                        = new EnrolmentStoreProxyService(mockEnrolmentStoreProxyConnector)

  "groupHasEnrolment" should {
    "return true when the list of group enrolments contains the ECL enrolment" in {
      val groupId = "group-123"

      val groupEnrolmentsWithEcl: GroupEnrolmentsResponseWithEcl =
        arbitrary[GroupEnrolmentsResponseWithEcl].sample.getOrElse {
          fail("Could not generate GroupEnrolmentsResponseWithEcl")
        }

      when(mockEnrolmentStoreProxyConnector.getEnrolmentsForGroup(ArgumentMatchers.eq(groupId))(any()))
        .thenReturn(Future.successful(groupEnrolmentsWithEcl.groupEnrolmentsResponse))

      val result = await(service.getEclReferenceFromGroupEnrolment(groupId).value)
      result shouldBe Right(groupEnrolmentsWithEcl.eclReferenceNumber)
    }

    "return EnrolmentStoreProxyError when the list of group enrolments does not contain the ECL enrolment" in forAll {
      (groupId: String, groupEnrolmentsWithoutEcl: GroupEnrolmentsResponseWithoutEcl) =>
        when(mockEnrolmentStoreProxyConnector.getEnrolmentsForGroup(ArgumentMatchers.eq(groupId))(any()))
          .thenReturn(Future.successful(groupEnrolmentsWithoutEcl.groupEnrolmentsResponse))

        val result = await(service.getEclReferenceFromGroupEnrolment(groupId).value)
        result shouldBe Left(EnrolmentStoreProxyError.BadGateway("Unable to find an ecl reference", BAD_GATEWAY))
    }

  }

}
