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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.EntityTypeDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.EntityTypeSelectedEvent
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{EntityTypeView, ErrorTemplate}
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityTypeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  storeUrl: StoreUrlAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: EntityTypeFormProvider,
  dataCleanup: EntityTypeDataCleanup,
  auditService: AuditService,
  view: EntityTypeView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[EntityType] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData andThen storeUrl) {
    implicit request =>
      Ok(view(form.prepare(request.registration.entityType), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        entityType => {
          val previousEntityType = request.registration.entityType

          val event = EntityTypeSelectedEvent(
            request.internalId,
            entityType
          ).extendedDataEvent

          val updatedRegistration = cleanupIfChange(request.registration, entityType, previousEntityType)

          (for {
            _ <- auditService.sendEvent(event).asResponseError
            _ <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
          } yield updatedRegistration).foldF(
            err => Future.successful(routeError(err)),
            _ =>
              mode match {
                case NormalMode => navigateInNormalMode(entityType)
                case CheckMode  => navigateInCheckMode(entityType, previousEntityType)
              }
          )
        }
      )
  }

  private def navigateInNormalMode(newEntityType: EntityType)(implicit request: RegistrationDataRequest[_]) =
    if (EntityType.isOther(newEntityType)) {
      Future.successful(Redirect(routes.BusinessNameController.onPageLoad(NormalMode)))
    } else {
      redirectToGRS(NormalMode, newEntityType)
    }

  private def navigateInCheckMode(newEntityType: EntityType, previousEntityType: Option[EntityType])(implicit
    request: RegistrationDataRequest[_]
  ) = {
    val sameEntityTypeAsPrevious = previousEntityType.contains(newEntityType)

    (sameEntityTypeAsPrevious, EntityType.isOther(newEntityType)) match {
      case (true, true)   => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
      case (false, true)  =>
        Future.successful(Redirect(routes.BusinessNameController.onPageLoad(CheckMode)))
      case (true, false)  =>
        (for {
          grsJourneyUrl <- eclRegistrationService.registerEntityType(newEntityType, NormalMode).asResponseError
        } yield grsJourneyUrl).fold(
          err => routeError(err),
          url => Redirect(Call(GET, url))
        )
      case (false, false) => redirectToGRS(CheckMode, newEntityType)
    }
  }

  private def redirectToGRS(
    mode: Mode,
    newEntityType: EntityType
  )(implicit request: RegistrationDataRequest[_]) =
    (for {
      grsJourneyUrl <- eclRegistrationService.registerEntityType(newEntityType, mode).asResponseError
    } yield grsJourneyUrl).fold(
      err => routeError(err),
      url => Redirect(Call(GET, url))
    )

  private def cleanupIfChange(
    registration: Registration,
    newEntityType: EntityType,
    previousEntityType: Option[EntityType]
  ) =
    previousEntityType match {
      case Some(value) if value == newEntityType  =>
        registration
      case _ if EntityType.isOther(newEntityType) =>
        dataCleanup
          .cleanupOtherEntityData(registration)
          .copy(entityType = Some(newEntityType))
      case _                                      =>
        dataCleanup
          .cleanup(registration)
          .copy(entityType = Some(newEntityType))
    }
}
