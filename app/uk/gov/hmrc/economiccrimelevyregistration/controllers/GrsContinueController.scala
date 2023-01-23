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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector,
  partnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector,
  eclRegistrationConnector: EclRegistrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  def continue(journeyId: String): Action[AnyContent] = (authorise andThen getRegistrationData).async {
    implicit request =>
      request.registration.entityType match {
        case Some(UkLimitedCompany) =>
          incorporatedEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(incorporatedEntityJourneyData = Some(jd))
              .flatMap(_ => handleGrsAndBvResult(jd.identifiersMatch, jd.businessVerification, jd.registration))
          }

        case Some(SoleTrader) =>
          soleTraderIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(soleTraderEntityJourneyData = Some(jd))
              .flatMap(_ => handleGrsAndBvResult(jd.identifiersMatch, jd.businessVerification, jd.registration))
          }

        case Some(GeneralPartnership) | Some(ScottishPartnership) | Some(LimitedPartnership) |
            Some(ScottishLimitedPartnership) | Some(LimitedLiabilityPartnership) =>
          partnershipIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(partnershipEntityJourneyData = Some(jd))
              .flatMap(_ => handleGrsAndBvResult(jd.identifiersMatch, jd.businessVerification, jd.registration))
          }

        case _ => throw new IllegalStateException("No valid entity type found in registration data")
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

  private def handleGrsAndBvResult(
    identifiersMatch: Boolean,
    bvResult: Option[BusinessVerificationResult],
    grsResult: GrsRegistrationResult
  )(implicit hc: HeaderCarrier): Future[Result] =
    (identifiersMatch, bvResult, grsResult.registrationStatus, grsResult.registeredBusinessPartnerId) match {
      case (false, _, _, _)                                  => Future.successful(Ok("Identifiers do not match"))
      case (_, Some(BusinessVerificationResult(Fail)), _, _) =>
        Future.successful(Ok("Failed business verification"))
      case (_, _, _, Some(businessPartnerId))                =>
        eclRegistrationConnector.getSubscriptionStatus(businessPartnerId).map {
          case EclSubscriptionStatus(NotSubscribed)                        => Redirect(routes.BusinessSectorController.onPageLoad(NormalMode))
          case EclSubscriptionStatus(Subscribed(eclRegistrationReference)) =>
            Ok(s"Business is already subscribed to ECL with registration reference $eclRegistrationReference")
        }
      case (_, _, RegistrationFailed, _)                     => Future.successful(Ok("Registration failed"))
      case _                                                 =>
        throw new IllegalStateException(
          s"Invalid result received from GRS: identifiersMatch: $identifiersMatch, registration: $grsResult, businessVerification: $bvResult"
        )
    }
}
