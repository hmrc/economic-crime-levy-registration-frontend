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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  storeUrl: StoreUrlAction,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector,
  partnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector,
  eclRegistrationConnector: EclRegistrationConnector
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler {

  def continue(mode: Mode, journeyId: String): Action[AnyContent] =
    (authorise andThen getRegistrationData andThen storeUrl).async { implicit request =>
      request.registration.entityType match {
        case Some(e @ (UkLimitedCompany | UnlimitedCompany | RegisteredSociety)) =>
          for {
            journeyData: IncorporatedEntityJourneyData <-
              incorporatedEntityIdentificationFrontendConnector.getJourneyData(journeyId)
            _                                          <- updateRegistrationWithJourneyData(incorporatedEntityJourneyData = Some(journeyData))
            result                                     <- handleGrsAndBvResult(
                                                            journeyData.identifiersMatch,
                                                            journeyData.businessVerification,
                                                            journeyData.registration,
                                                            e,
                                                            mode
                                                          )
          } yield result
        case Some(e @ SoleTrader)                                                =>
          for {
            journeyData <- soleTraderIdentificationFrontendConnector.getJourneyData(journeyId)
            _           <- updateRegistrationWithJourneyData(soleTraderEntityJourneyData = Some(journeyData))
            result      <- handleGrsAndBvResult(
                             journeyData.identifiersMatch,
                             journeyData.businessVerification,
                             journeyData.registration,
                             e,
                             mode
                           )
          } yield result

        case Some(
              e @ (GeneralPartnership | ScottishPartnership | LimitedPartnership | ScottishLimitedPartnership |
              LimitedLiabilityPartnership)
            ) =>
          partnershipIdentificationFrontendConnector.getJourneyData(journeyId).flatMap { jd =>
            updateRegistrationWithJourneyData(partnershipEntityJourneyData = Some(jd))
              .flatMap(_ =>
                handleGrsAndBvResult(
                  jd.identifiersMatch,
                  jd.businessVerification,
                  jd.registration,
                  e,
                  mode
                )
              )
          }

        case None =>
          Future.successful(
            routeError(ResponseError.badRequestError("No entity type found in registration data"))
          )

        case _ =>
          Future.successful(
            routeError(ResponseError.internalServiceError("No valid entity type found in registration data"))
          )
      }
    }

  private def updateRegistrationWithJourneyData(
    incorporatedEntityJourneyData: Option[IncorporatedEntityJourneyData] = None,
    soleTraderEntityJourneyData: Option[SoleTraderEntityJourneyData] = None,
    partnershipEntityJourneyData: Option[PartnershipEntityJourneyData] = None
  )(implicit request: RegistrationDataRequest[AnyContent]): Future[Unit] =
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
  )(implicit hc: HeaderCarrier, request: RegistrationDataRequest[_]): Future[Result] =
    (identifiersMatch, bvResult, grsResult.registrationStatus, grsResult.registeredBusinessPartnerId) match {
      case (false, _, _, _)                                  => Future.successful(Redirect(routes.NotableErrorController.verificationFailed()))
      case (_, Some(BusinessVerificationResult(Fail)), _, _) =>
        Future.successful(Redirect(routes.NotableErrorController.verificationFailed()))
      case (_, _, _, Some(businessPartnerId))                =>
        eclRegistrationConnector.getSubscriptionStatus(businessPartnerId).map {
          case EclSubscriptionStatus(NotSubscribed)                          =>
            mode match {
              case NormalMode =>
                entityType match {
                  case GeneralPartnership | ScottishPartnership =>
                    Redirect(routes.PartnershipNameController.onPageLoad(NormalMode))
                  case _                                        => Redirect(routes.BusinessSectorController.onPageLoad(NormalMode))
                }
              case CheckMode  =>
                Redirect(entityType match {
                  case GeneralPartnership | ScottishPartnership =>
                    if (request.registration.partnershipName.isEmpty) {
                      routes.PartnershipNameController.onPageLoad(mode)
                    } else {
                      routes.CheckYourAnswersController
                        .onPageLoad()
                    }
                  case _                                        =>
                    routes.CheckYourAnswersController
                      .onPageLoad()
                })
            }
          case EclSubscriptionStatus(Subscribed(eclRegistrationReference))   =>
            Redirect(routes.NotableErrorController.organisationAlreadyRegistered(eclRegistrationReference))
          case EclSubscriptionStatus(DeRegistered(eclRegistrationReference)) =>
            routeError(
              ResponseError.internalServiceError(
                s"ECL Subscription is deregistered for $eclRegistrationReference"
              )
            )
        }
      case (_, _, RegistrationFailed, _)                     =>
        grsResult.failures match {
          case Some(failures) if failures.exists(_.code == GrsErrorCodes.partyTypeMismatch) =>
            Future.successful(Redirect(routes.NotableErrorController.partyTypeMismatch()))
          case _                                                                            =>
            Future.successful(Redirect(routes.NotableErrorController.registrationFailed()))
        }
      case _                                                 =>
        Future.successful(
          routeError(
            ResponseError.internalServiceError(
              s"Invalid result received from GRS: identifiersMatch: $identifiersMatch, registration: $grsResult, businessVerification: $bvResult"
            )
          )
        )
    }
}
