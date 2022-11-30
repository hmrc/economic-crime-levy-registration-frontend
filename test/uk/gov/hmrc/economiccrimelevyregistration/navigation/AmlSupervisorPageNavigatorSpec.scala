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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, NormalMode, Registration}

class AmlSupervisorPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AmlSupervisorPageNavigator

  "nextPage" should {
    "return a Call to the register with your AML Supervisor page in NormalMode when either the Gambling Commission or Financial Conduct Authority AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType      = Gen.oneOf[AmlSupervisorType](Seq(GamblingCommission, FinancialConductAuthority)).sample.get
        val amlSupervisor       = AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = None)
        val updatedRegistration = registration.copy(amlSupervisor = Some(amlSupervisor))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.RegisterWithOtherAmlSupervisorController
          .onPageLoad()
    }

    "return a Call to the entity type page in NormalMode when either the HMRC or Other AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
        val amlSupervisor         =
          AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
        val updatedRegistration   = registration.copy(amlSupervisor = Some(amlSupervisor))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.EntityTypeController.onPageLoad()
    }
  }

}
