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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class AmlSupervisorPageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new AmlSupervisorPageNavigator()

  "nextPage" should {
    "return a Call to the register with GC page in NormalMode when the Gambling Commission option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          amlSupervisor = Some(AmlSupervisor(GamblingCommission, None)),
          registrationType = Some(Initial)
        )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.RegisterWithGcController.onPageLoad()
    }

    "return a Call to the register with GC page in CheckMode when the Gambling Commission option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          amlSupervisor = Some(AmlSupervisor(GamblingCommission, None)),
          registrationType = Some(Initial)
        )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.RegisterWithGcController.onPageLoad()
    }

    "return a Call to the register with FCA page in NormalMode when the Financial Conduct Authority option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            amlSupervisor = Some(AmlSupervisor(FinancialConductAuthority, None)),
            registrationType = Some(Initial)
          )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.RegisterWithFcaController.onPageLoad()
    }

    "return a Call to the register with FCA page in CheckMode when the Financial Conduct Authority option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            amlSupervisor = Some(AmlSupervisor(FinancialConductAuthority, None)),
            registrationType = Some(Initial)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.RegisterWithFcaController.onPageLoad()
    }

    "return a Call to the relevant AP 12 months page in NormalMode when either the HMRC or Other AML Supervisor option is selected" +
      "and the user has carried out Aml regulated activity in current FY" in forAll { registration: Registration =>
        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
        val amlSupervisor         =
          AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
        val updatedRegistration   =
          registration.copy(
            carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
            amlSupervisor = Some(amlSupervisor),
            registrationType = Some(Initial)
          )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.RelevantAp12MonthsController.onPageLoad(NormalMode)
      }

    "return a Call to the check your answers page in CheckMode when either the HMRC or Other AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
        val amlSupervisor         =
          AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
        val updatedRegistration   =
          registration.copy(amlSupervisor = Some(amlSupervisor), registrationType = Some(Initial))

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }
  }

  "return a Call to the entity type page in NormalMode when either the HMRC or Other AML Supervisor option is selected" +
    "and the user has NOT carried out Aml regulated activity in current FY" in forAll { registration: Registration =>
      val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
      val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
      val amlSupervisor         =
        AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
      val updatedRegistration   =
        registration.copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(false),
          amlSupervisor = Some(amlSupervisor),
          registrationType = Some(Initial)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.EntityTypeController.onPageLoad(NormalMode)
    }

}
