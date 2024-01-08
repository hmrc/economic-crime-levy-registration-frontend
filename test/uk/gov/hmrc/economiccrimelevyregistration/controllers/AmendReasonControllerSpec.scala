/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendReasonView
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmendReasonPageNavigator

import scala.concurrent.Future

class AmendReasonControllerSpec extends SpecBase {

  val view: AmendReasonView                       = app.injector.instanceOf[AmendReasonView]
  val formProvider: AmendReasonFormProvider       = new AmendReasonFormProvider()
  val form: Form[String]                          = formProvider()
  val registrationService: EclRegistrationService = mock[EclRegistrationService]
  val pageNavigator: AmendReasonPageNavigator     = new AmendReasonPageNavigator() {}

  class TestContext(registration: Registration) {
    val controller = new AmendReasonController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registration.internalId),
      fakeDataRetrievalAction(registration),
      view,
      formProvider,
      registrationService,
      pageNavigator
    )
  }

  "onPageLoad" should {
    "return OK and correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(amendReason = None)) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode).toString
      }
    }

    "return OK and correct view when answer has already been provided" in forAll {
      (registration: Registration, reason: String) =>
        new TestContext(registration.copy(amendReason = Some(reason))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(reason), NormalMode)
        }
    }
  }
}
