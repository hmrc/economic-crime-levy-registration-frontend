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

package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType._
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, Registration}

class RegisterWithOtherAmlSupervisorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RegisterWithOtherAmlSupervisorController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.RegisterWithOtherAmlSupervisorController.onPageLoad())

    "respond with 200 status and the financial conduct authority HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration.copy(amlSupervisor = Some(AmlSupervisor(FinancialConductAuthority, None))))

      val result = callRoute(FakeRequest(routes.RegisterWithOtherAmlSupervisorController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("You need to register with the FCA")
    }

    "respond with 200 status and the gambling commission HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration.copy(amlSupervisor = Some(AmlSupervisor(GamblingCommission, None))))

      val result = callRoute(FakeRequest(routes.RegisterWithOtherAmlSupervisorController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("You need to register with the GC")
    }
  }

}
