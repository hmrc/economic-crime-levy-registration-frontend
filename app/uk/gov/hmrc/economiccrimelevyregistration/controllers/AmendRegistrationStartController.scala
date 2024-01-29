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

import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.CheckMode
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendRegistrationStartView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendRegistrationStartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  authorise: AuthorisedActionWithEnrolmentCheck,
  errorHandler: ErrorHandler,
  view: AmendRegistrationStartView,
  registrationService: EclRegistrationService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(eclReference: String): Action[AnyContent] = authorise.async { implicit request =>
    val createOrUpdateRegistration = for {
      _        <- getOrCreateRegistration(request.internalId)
      response <- getOrCreateRegistrationAdditionalInfo(eclReference)
    } yield response

    createOrUpdateRegistration
      .flatMap { _ =>
        if (appConfig.getSubscriptionEnabled) {
          routeToAmendReason(eclReference, request.internalId)
        } else {
          Future.successful(Ok(view(eclReference)))
        }
      }
      .recover { case e =>
        logger.error(
          s"Failed to create registration additional info for eclReference $eclReference. Error message: ${e.getMessage}"
        )
        InternalServerError(errorHandler.internalServerErrorTemplate)
      }
  }

  private def routeToAmendReason(eclRegistrationReference: String, internalId: String)(implicit
    hc: HeaderCarrier
  ): Future[Result] = {
    val result = for {
      registration            <- registrationService.getOrCreateRegistration(internalId)
      getSubscriptionResponse <- registrationService.getSubscription(eclRegistrationReference)
      transformedRegistration  = registrationService.transformToRegistration(registration, getSubscriptionResponse)
      upsertedRegistration    <- registrationService.upsertRegistration(transformedRegistration)
    } yield upsertedRegistration

    result
      .map(_ => Redirect(routes.AmendReasonController.onPageLoad(CheckMode).url))
      .recover { case e =>
        throw e
      }

  }

  private def getOrCreateRegistration(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      registration <- registrationService.getOrCreateRegistration(internalId)
      _            <- registrationService.upsertRegistration(registration.copy(registrationType = Some(Amendment)))
    } yield ()

  private def getOrCreateRegistrationAdditionalInfo(
    eclReference: String
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[AnyContent]): Future[Unit] =
    registrationAdditionalInfoService
      .createOrUpdate(request.internalId, Some(eclReference))
}
