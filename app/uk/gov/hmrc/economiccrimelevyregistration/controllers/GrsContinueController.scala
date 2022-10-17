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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, PartnershipEntityIdentificationFrontendConnector, SoleTraderEntityIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderEntityIdentificationFrontendConnector: SoleTraderEntityIdentificationFrontendConnector,
  partnershipEntityIdentificationFrontendConnector: PartnershipEntityIdentificationFrontendConnector,
  eclRegistrationConnector: EclRegistrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  def continue(journeyId: String): Action[AnyContent] = (authorise andThen getRegistrationData).async {
    implicit request =>
      request.registration.entityType match {
        case Some(UkLimitedCompany) =>
          incorporatedEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(incorporatedEntityJourneyData = Some(jd))
              .map(_ => Ok(Json.toJson(jd)))
          }

        case Some(SoleTrader) =>
          soleTraderEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(soleTraderEntityJourneyData = Some(jd))
              .map(_ => Ok(Json.toJson(jd)))
          }

        case Some(GeneralPartnership) | Some(ScottishPartnership) | Some(LimitedPartnership) |
            Some(ScottishLimitedPartnership) | Some(LimitedLiabilityPartnership) =>
          partnershipEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(partnershipEntityJourneyData = Some(jd))
              .map(_ => Ok(Json.toJson(jd)))
          }

        case None => throw new IllegalStateException("No entity type found in registration data")
      }
  }

  private def updateRegistrationWithJourneyData(
    incorporatedEntityJourneyData: Option[IncorporatedEntityJourneyData] = None,
    soleTraderEntityJourneyData: Option[SoleTraderEntityJourneyData] = None,
    partnershipEntityJourneyData: Option[PartnershipEntityJourneyData] = None
  )(implicit request: RegistrationDataRequest[AnyContent]): Future[Registration] =
    eclRegistrationConnector.upsertRegistration(
      request.registration.copy(
        incorporatedEntityJourneyData = incorporatedEntityJourneyData,
        soleTraderEntityJourneyData = soleTraderEntityJourneyData,
        partnershipEntityJourneyData = partnershipEntityJourneyData
      )
    )
}
