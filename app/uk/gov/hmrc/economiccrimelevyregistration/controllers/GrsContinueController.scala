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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, SoleTraderEntityIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.{SoleTrader, UkLimitedCompany}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GrsContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderEntityIdentificationFrontendConnector: SoleTraderEntityIdentificationFrontendConnector,
  eclRegistrationConnector: EclRegistrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  def continue(journeyId: String): Action[AnyContent] = (authorise andThen getRegistrationData).async {
    implicit request =>
      request.registration.entityType match {
        case Some(UkLimitedCompany) =>
          incorporatedEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            eclRegistrationConnector
              .upsertRegistration(request.registration.copy(incorporatedEntityJourneyData = Some(jd)))
              .map { _ =>
                Ok(Json.toJson(jd))
              }
          }

        case Some(SoleTrader) =>
          soleTraderEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            eclRegistrationConnector
              .upsertRegistration(request.registration.copy(soleTraderEntityJourneyData = Some(jd)))
              .map { _ =>
                Ok(Json.toJson(jd))
              }
          }
      }
  }
}
