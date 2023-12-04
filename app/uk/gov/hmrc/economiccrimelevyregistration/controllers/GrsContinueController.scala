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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
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
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector,
  partnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector,
  eclRegistrationConnector: EclRegistrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  def continue(mode: Mode, journeyId: String): Action[AnyContent] = (authorise andThen getRegistrationData).async {
    implicit request =>
      request.registration.entityType match {
        case Some(e @ (UkLimitedCompany | UnlimitedCompany | RegisteredSociety)) =>
          incorporatedEntityIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(incorporatedEntityJourneyData = Some(jd))
              .flatMap(_ =>
                handleGrsAndBvResult(jd.identifiersMatch, jd.businessVerification, jd.registration, e, mode)
              )
          }

        case Some(e @ SoleTrader) =>
          soleTraderIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(soleTraderEntityJourneyData = Some(jd))
              .flatMap(_ =>
                handleGrsAndBvResult(jd.identifiersMatch, jd.businessVerification, jd.registration, e, mode)
              )
          }

        case Some(
              e @ (GeneralPartnership | ScottishPartnership | LimitedPartnership | ScottishLimitedPartnership |
              LimitedLiabilityPartnership)
            ) =>
          partnershipIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(partnershipEntityJourneyData = Some(jd))
              .flatMap(_ =>
                handleGrsAndBvResult(jd.identifiersMatch, jd.businessVerification, jd.registration, e, mode)
              )
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
    grsResult: GrsRegistrationResult,
    entityType: EntityType,
    mode: Mode
  )(implicit hc: HeaderCarrier): Future[Result] =
    (identifiersMatch, bvResult, grsResult.registrationStatus, grsResult.registeredBusinessPartnerId) match {
      case (false, _, _, _)                                  => Future.successful(Redirect(routes.NotableErrorController.verificationFailed()))
      case (_, Some(BusinessVerificationResult(Fail)), _, _) =>
        Future.successful(Redirect(routes.NotableErrorController.verificationFailed()))
      case (_, _, _, Some(businessPartnerId))                =>
        eclRegistrationConnector.getSubscriptionStatus(businessPartnerId).map {
          case EclSubscriptionStatus(NotSubscribed)                        =>
            mode match {
              case NormalMode =>
                entityType match {
                  case GeneralPartnership | ScottishPartnership =>
                    Redirect(routes.PartnershipNameController.onPageLoad(NormalMode))
                  case _                                        => Redirect(routes.BusinessSectorController.onPageLoad(NormalMode))
                }
              case CheckMode  => Redirect(routes.CheckYourAnswersController.onPageLoad())
            }
          case EclSubscriptionStatus(Subscribed(eclRegistrationReference)) =>
            Redirect(routes.NotableErrorController.organisationAlreadyRegistered(eclRegistrationReference))
        }
      case (_, _, RegistrationFailed, _)                     =>
        grsResult.failures match {
          case Some(failures) if failures.exists(_.code == GrsErrorCodes.PartyTypeMismatch) =>
            Future.successful(Redirect(routes.NotableErrorController.partyTypeMismatch()))
          case _                                                                            =>
            Future.successful(Redirect(routes.NotableErrorController.registrationFailed()))
        }
      case _                                                 =>
        throw new IllegalStateException(
          s"Invalid result received from GRS: identifiersMatch: $identifiersMatch, registration: $grsResult, businessVerification: $bvResult"
        )
    }
}
