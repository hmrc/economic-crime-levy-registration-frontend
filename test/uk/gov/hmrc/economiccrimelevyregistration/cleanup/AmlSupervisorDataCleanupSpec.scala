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

package uk.gov.hmrc.economiccrimelevyregistration.cleanup

import org.scalacheck.Arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.{EligibleAmlSupervisor, IneligibleAmlSupervisor}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

class AmlSupervisorDataCleanupSpec extends SpecBase {

  implicit val arbEligibleAmlSupervisor: Arbitrary[EligibleAmlSupervisor] = arbEligibleAmlSupervisor(appConfig)

  val dataCleaner = new AmlSupervisorDataCleanup

  "cleanup" should {
    "return a registration with no answers apart from the AML regulated activity and AML supervisor answers when the answer is FCA or GC" in forAll {
      (registration: Registration, ineligibleAmlSupervisor: IneligibleAmlSupervisor) =>
        val updatedRegistration = registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(false),
          amlSupervisor = Some(ineligibleAmlSupervisor.amlSupervisor)
        )

        dataCleaner.cleanup(updatedRegistration) shouldBe Registration
          .empty(registration.internalId)
          .copy(
            carriedOutAmlRegulatedActivityInCurrentFy = updatedRegistration.carriedOutAmlRegulatedActivityInCurrentFy,
            amlSupervisor = updatedRegistration.amlSupervisor
          )
    }

    "return a registration with all existing answers when the answer is either HMRC or one of the other professional bodies" in forAll {
      (registration: Registration, eligibleAmlSupervisor: EligibleAmlSupervisor) =>
        val updatedRegistration = registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
          amlSupervisor = Some(eligibleAmlSupervisor.amlSupervisor)
        )

        dataCleaner.cleanup(updatedRegistration) shouldBe updatedRegistration
    }
  }

}
