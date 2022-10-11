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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, SoleTraderEntityIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Partnership, SoleTrader, UkLimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.EntityTypeView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityTypeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderEntityIdentificationFrontendConnector: SoleTraderEntityIdentificationFrontendConnector,
  eclRegistrationConnector: EclRegistrationConnector,
  formProvider: EntityTypeFormProvider,
  view: EntityTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[EntityType] = formProvider()

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(view(form))
  }

  def onSubmit: Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        entityType =>
          eclRegistrationConnector
            .upsertRegistration(request.registration.copy(entityType = Some(entityType)))
            .flatMap { _ =>
              entityType match {
                case UkLimitedCompany =>
                  incorporatedEntityIdentificationFrontendConnector
                    .createLimitedCompanyJourney()
                    .map(createJourneyResponse => Redirect(createJourneyResponse.journeyStartUrl))
                case SoleTrader       =>
                  soleTraderEntityIdentificationFrontendConnector
                    .createSoleTraderJourney()
                    .map(createJourneyResponse => Redirect(createJourneyResponse.journeyStartUrl))
                case Partnership      => Future.successful(Ok("Howdy Partner"))
              }
            }
      )
  }
}
