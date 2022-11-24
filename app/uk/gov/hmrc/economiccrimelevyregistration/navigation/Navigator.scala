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

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.pages.{AmlSupervisorPage, Page, UkRevenuePage}

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => Registration => Call = {
    case UkRevenuePage     => ukRevenueRoute
    case AmlSupervisorPage => amlSupervisorRoute
    case _                 => _ => routes.StartController.onPageLoad()
  }

  private val checkRouteMap: Page => Registration => Call = { case _ =>
    _ => routes.CheckYourAnswersController.onPageLoad()
  }

  private def ukRevenueRoute(registration: Registration): Call =
    registration.meetsRevenueThreshold match {
      case Some(true)  => routes.AmlSupervisorController.onPageLoad()
      case Some(false) => routes.NotLiableController.onPageLoad()
      case _           => routes.StartController.onPageLoad()
    }

  private def amlSupervisorRoute(registration: Registration): Call =
    registration.amlSupervisor match {
      case Some(AmlSupervisor(GamblingCommission, _)) | Some(AmlSupervisor(FinancialConductAuthority, _)) =>
        routes.RegisterWithOtherAmlSupervisorController.onPageLoad()
      case Some(AmlSupervisor(Hmrc, _)) | Some(AmlSupervisor(Other, _))                                   => routes.EntityTypeController.onPageLoad()
      case _                                                                                              => routes.StartController.onPageLoad()
    }

  def nextPage(page: Page, mode: Mode, registration: Registration): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(registration)
    case CheckMode  =>
      checkRouteMap(page)(registration)
  }
}
