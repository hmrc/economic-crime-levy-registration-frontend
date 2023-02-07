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

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType._
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisorType, NormalMode, Registration}

import javax.inject.Inject

class AmlSupervisorPageNavigator @Inject() () extends PageNavigator {

  override protected def navigateInNormalMode(
    registration: Registration
  ): Call =
    registration.amlSupervisor match {
      case Some(amlSupervisor) =>
        amlSupervisor.supervisorType match {
          case t @ (GamblingCommission | FinancialConductAuthority) => registerWithGcOrFca(t)
          case Hmrc | Other                                         => routes.RelevantAp12MonthsController.onPageLoad(NormalMode)
        }
      case _                   => routes.NotableErrorController.answersAreInvalid()
    }

  override protected def navigateInCheckMode(
    registration: Registration
  ): Call =
    registration.amlSupervisor match {
      case Some(amlSupervisor) =>
        amlSupervisor.supervisorType match {
          case t @ (GamblingCommission | FinancialConductAuthority) => registerWithGcOrFca(t)
          case Hmrc | Other                                         => routes.CheckYourAnswersController.onPageLoad()
        }
      case _                   => routes.NotableErrorController.answersAreInvalid()
    }

  private def registerWithGcOrFca(amlSupervisorType: AmlSupervisorType): Call =
    amlSupervisorType match {
      case GamblingCommission        => routes.RegisterWithGcController.onPageLoad()
      case FinancialConductAuthority => routes.RegisterWithFcaController.onPageLoad()
      case _                         => routes.NotableErrorController.answersAreInvalid()
    }

}
