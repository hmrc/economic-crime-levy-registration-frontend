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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisor
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{FinancialConductAuthorityView, GamblingCommissionView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class RegisterWithOtherAmlSupervisorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  gcView: GamblingCommissionView,
  fcaView: FinancialConductAuthorityView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    request.registration.amlSupervisor match {
      case Some(AmlSupervisor(GamblingCommission, _))        => Ok(gcView())
      case Some(AmlSupervisor(FinancialConductAuthority, _)) => Ok(fcaView())
      case _                                                 =>
        throw new IllegalStateException(
          "The AML Supervisor was not either GamblingCommission or FinancialConductAuthority as expected"
        )
    }
  }
}
