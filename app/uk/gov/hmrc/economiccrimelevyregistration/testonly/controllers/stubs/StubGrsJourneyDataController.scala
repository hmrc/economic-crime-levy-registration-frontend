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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.controllers.stubs

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.testonly.data.GrsStubData
import uk.gov.hmrc.economiccrimelevyregistration.testonly.forms.GrsStubFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.testonly.models.GrsStubFormData
import uk.gov.hmrc.economiccrimelevyregistration.testonly.utils.Base64Utils
import uk.gov.hmrc.economiccrimelevyregistration.testonly.views.html.StubGrsJourneyDataView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class StubGrsJourneyDataController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  grsStubFormProvider: GrsStubFormProvider,
  view: StubGrsJourneyDataView
) extends FrontendBaseController
    with GrsStubData
    with I18nSupport {

  val form: Form[GrsStubFormData] = grsStubFormProvider()

  private val registrationSuccessBvDisabledF: EntityType => String           =
    constructGrsStubFormData(_, None, registered, identifiersMatch = true)
  private val registrationSuccessBvEnabledF: EntityType => String            =
    constructGrsStubFormData(_, bvPassed, registered, identifiersMatch = true)
  private val registrationFailedPartyTypeMismatchF: EntityType => String     =
    constructGrsStubFormData(_, None, registrationFailedPartyTypeMismatch, identifiersMatch = true)
  private val registrationFailedGenericF: EntityType => String               =
    constructGrsStubFormData(_, None, registrationFailedGeneric, identifiersMatch = true)
  private val registrationNotCalledIdentifierMismatchF: EntityType => String =
    constructGrsStubFormData(_, None, registrationNotCalled, identifiersMatch = false)
  private val registrationNotCalledBvFailedF: EntityType => String           =
    constructGrsStubFormData(_, bvFailed, registrationNotCalled, identifiersMatch = true)

  def onPageLoad(continueUrl: String, entityType: String): Action[AnyContent] = Action { implicit request =>
    val e: EntityType = EntityType.enumerable.value(entityType).get

    Ok(
      view(
        form,
        registrationSuccessBvDisabledF(e),
        registrationSuccessBvEnabledF(e),
        registrationFailedPartyTypeMismatchF(e),
        registrationFailedGenericF(e),
        registrationNotCalledIdentifierMismatchF(e),
        registrationNotCalledBvFailedF(e),
        continueUrl,
        entityType
      )
    )
  }

  def onSubmit(continueUrl: String, entityType: String): Action[AnyContent] = (authorise andThen getRegistrationData) {
    implicit request =>
      val e: EntityType = EntityType.enumerable.value(entityType).get

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            BadRequest(
              view(
                formWithErrors,
                registrationSuccessBvDisabledF(e),
                registrationSuccessBvEnabledF(e),
                registrationFailedPartyTypeMismatchF(e),
                registrationFailedGenericF(e),
                registrationNotCalledIdentifierMismatchF(e),
                registrationNotCalledBvFailedF(e),
                continueUrl,
                entityType
              )
            ),
          grsStubFormData =>
            Redirect(
              s"/register-for-economic-crime-levy/grs-continue/" +
                s"$continueUrl?journeyId=${Base64Utils
                    .base64UrlEncode(grsStubFormData.grsJourneyDataJson)}"
            )
        )
  }

}
