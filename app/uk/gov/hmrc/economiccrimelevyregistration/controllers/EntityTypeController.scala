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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.EntityTypeDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.EntityTypeSelectedEvent
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{EntityTypePageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{EntityTypeView, ErrorTemplate}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityTypeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: EntityTypeFormProvider,
  pageNavigator: EntityTypePageNavigator,
  dataCleanup: EntityTypeDataCleanup,
  auditConnector: AuditConnector,
  view: EntityTypeView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[EntityType] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(view(form.prepare(request.registration.entityType), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        entityType => {
          val sameEntityType = request.registration.entityType match {
            case Some(value) => value == entityType
            case None        => false
          }

          auditConnector
            .sendExtendedEvent(
              EntityTypeSelectedEvent(
                request.internalId,
                entityType
              ).extendedDataEvent
            )

          val updatedRegistration = cleanup(mode, request.registration, entityType)

          (for {
            upsertedRegistration <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
            grsJourneyUrl        <- eclRegistrationService.registerEntityType(entityType, mode, sameEntityType).asResponseError
          } yield NavigationData(
            registration = upsertedRegistration,
            url = grsJourneyUrl,
            isSame = sameEntityType
          )).convertToResult(mode, pageNavigator)
        }
      )
  }

  private def cleanup(
    mode: Mode,
    registration: Registration,
    entityType: EntityType
  ) = {
    val previousEntityType = registration.entityType
    val isOther            = EntityType.isOther(entityType)
    if (previousEntityType.contains(entityType) && mode == CheckMode && isOther) {
      registration
    } else if (!isOther) {
      dataCleanup.cleanup(
        registration.copy(
          entityType = Some(entityType)
        )
      )
    } else {
      previousEntityType match {
        case Some(value) if value == entityType =>
          dataCleanup.cleanup(
            registration.copy(entityType = Some(entityType))
          )
        case _                                  =>
          dataCleanup.cleanupOtherEntityData(
            registration.copy(entityType = Some(entityType))
          )
      }
    }
  }
}
