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

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EntityTypePageNavigator @Inject() (
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector,
  partnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector
)(implicit ec: ExecutionContext)
    extends AsyncPageNavigator
    with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(
    registration: Registration
  )(implicit request: RequestHeader): Future[Call] =
    registration.entityType match {
      case Some(entityType) =>
        entityType match {
          case UkLimitedCompany =>
            incorporatedEntityIdentificationFrontendConnector
              .createLimitedCompanyJourney()
              .map(createJourneyResponse => Call(GET, createJourneyResponse.journeyStartUrl))

          case SoleTrader =>
            soleTraderIdentificationFrontendConnector
              .createSoleTraderJourney()
              .map(createJourneyResponse => Call(GET, createJourneyResponse.journeyStartUrl))

          case GeneralPartnership | ScottishPartnership | LimitedPartnership | ScottishLimitedPartnership |
              LimitedLiabilityPartnership =>
            partnershipIdentificationFrontendConnector
              .createPartnershipJourney(entityType)
              .map(createJourneyResponse => Call(GET, createJourneyResponse.journeyStartUrl))

          case Other => ???
        }
      case _                => Future.successful(routes.JourneyRecoveryController.onPageLoad())
    }

  override protected def navigateInCheckMode(registration: Registration): Future[Call] = ???

  override def previousPage(registration: Registration): Call = ???
}
