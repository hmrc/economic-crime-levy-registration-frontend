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

import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.views.html.FinancialConductAuthorityView

import scala.concurrent.Future

class RegisterWithFcaControllerSpec extends SpecBase {

  val view: FinancialConductAuthorityView = app.injector.instanceOf[FinancialConductAuthorityView]

  val controller = new RegisterWithFcaController(
    mcc,
    fakeAuthorisedActionWithEnrolmentCheck("test-internal-id"),
    view
  )

  "onPageLoad" should {
    "return OK and correct view" in {
      val result: Future[Result] = controller.onPageLoad()(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe view()(fakeRequest, messages).toString
    }
  }
}
