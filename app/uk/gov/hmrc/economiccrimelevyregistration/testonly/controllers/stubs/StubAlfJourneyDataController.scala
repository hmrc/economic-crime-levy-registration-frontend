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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.{AlfAddress, AlfAddressData, AlfCountry}
import uk.gov.hmrc.economiccrimelevyregistration.testonly.forms.AlfStubFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.testonly.models.AlfStubFormData
import uk.gov.hmrc.economiccrimelevyregistration.testonly.utils.Base64Utils
import uk.gov.hmrc.economiccrimelevyregistration.testonly.views.html.StubAlfJourneyDataView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class StubAlfJourneyDataController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  alfStubFormProvider: AlfStubFormProvider,
  view: StubAlfJourneyDataView
) extends FrontendBaseController
    with I18nSupport {

  val form: Form[AlfStubFormData] = alfStubFormProvider()

  def onPageLoad(continueUrl: String): Action[AnyContent] = Action { implicit request =>
    val defaultAddressJson = Json.toJson(
      AlfAddressData(
        id = None,
        address = AlfAddress(
          organisation = None,
          lines = Seq("Address Line 1", "Address Line 2", "Address Line 3", "Test Town"),
          postcode = Some("AB12 3CD"),
          country = AlfCountry("GB", "United Kingdom"),
          poBox = None
        )
      )
    )

    Ok(view(form.fill(AlfStubFormData(Json.prettyPrint(defaultAddressJson))), continueUrl))
  }

  def onSubmit(continueUrl: String): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(view(formWithErrors, continueUrl)),
        alfStubFormData =>
          Redirect(
            s"/register-for-economic-crime-levy/address-lookup-continue/$continueUrl?id=${Base64Utils
              .base64UrlEncode(alfStubFormData.addressJson)}"
          )
      )
  }

}
