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

import org.mockito.ArgumentMatchers.any
import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models._

import scala.concurrent.Future

class AmlSupervisorPageNavigatorSpec extends SpecBase {

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val pageNavigator                                          = new AmlSupervisorPageNavigator(mockEclRegistrationConnector)

  "nextPage" should {
    "return a Call to the register with GC page in NormalMode when the Gambling Commission option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(amlSupervisor = Some(AmlSupervisor(GamblingCommission, None)))

        when(mockEclRegistrationConnector.deleteRegistration(any())(any())).thenReturn(Future.successful(()))

        await(
          pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.RegisterWithGcController
          .onPageLoad()
    }

    "return a Call to the register with GC page in CheckMode when the Gambling Commission option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(amlSupervisor = Some(AmlSupervisor(GamblingCommission, None)))

        when(mockEclRegistrationConnector.deleteRegistration(any())(any())).thenReturn(Future.successful(()))

        await(
          pageNavigator.nextPage(CheckMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.RegisterWithGcController
          .onPageLoad()
    }

    "return a Call to the register with FCA page in NormalMode when the Financial Conduct Authority option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(amlSupervisor = Some(AmlSupervisor(FinancialConductAuthority, None)))

        when(mockEclRegistrationConnector.deleteRegistration(any())(any())).thenReturn(Future.successful(()))

        await(
          pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.RegisterWithFcaController
          .onPageLoad()
    }

    "return a Call to the register with FCA page in CheckMode when the Financial Conduct Authority option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(amlSupervisor = Some(AmlSupervisor(FinancialConductAuthority, None)))

        when(mockEclRegistrationConnector.deleteRegistration(any())(any())).thenReturn(Future.successful(()))

        await(
          pageNavigator.nextPage(CheckMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.RegisterWithFcaController
          .onPageLoad()
    }

    "return a Call to the relevant AP 12 months page in NormalMode when either the HMRC or Other AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
        val amlSupervisor         =
          AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
        val updatedRegistration   = registration.copy(amlSupervisor = Some(amlSupervisor))

        await(
          pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.RelevantAp12MonthsController
          .onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode when either the HMRC or Other AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
        val amlSupervisor         =
          AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
        val updatedRegistration   = registration.copy(amlSupervisor = Some(amlSupervisor))

        await(
          pageNavigator.nextPage(CheckMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.CheckYourAnswersController
          .onPageLoad()
    }
  }

}
