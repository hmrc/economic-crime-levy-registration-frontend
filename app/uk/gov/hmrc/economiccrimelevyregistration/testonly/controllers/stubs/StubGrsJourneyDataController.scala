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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.controllers.stubs

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.testonly.forms.JourneyIdFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.testonly.views.html.StubGrsJourneyDataView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class StubGrsJourneyDataController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  journeyIdFormProvider: JourneyIdFormProvider,
  view: StubGrsJourneyDataView
) extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = journeyIdFormProvider()

  def onPageLoad(): Action[AnyContent] = Action { implicit request =>
    Ok(view(form.fill("0")))
  }

  def onSubmit(): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        journeyId =>
          Redirect(
            s"/register-for-economic-crime-levy/grs-continue?journeyId=$journeyId-${request.registration.entityType.get.toString}"
          )
      )
  }

}