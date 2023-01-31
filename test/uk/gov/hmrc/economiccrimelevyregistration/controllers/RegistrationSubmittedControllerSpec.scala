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

import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RegistrationSubmittedView

import scala.concurrent.Future

class RegistrationSubmittedControllerSpec extends SpecBase {

  val view: RegistrationSubmittedView = app.injector.instanceOf[RegistrationSubmittedView]

  val controller = new RegistrationSubmittedController(
    mcc,
    fakeAuthorisedActionWithoutEnrolmentCheck,
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll { eclReference: String =>
      val result: Future[Result] =
        controller.onPageLoad()(fakeRequest.withSession((SessionKeys.EclReference, eclReference)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe view(eclReference)(fakeRequest, messages).toString
    }

    "throw an IllegalStateException when the ECL reference is not found in the sessions" in {
      val result: IllegalStateException = intercept[IllegalStateException] {
        await(controller.onPageLoad()(fakeRequest))
      }

      result.getMessage shouldBe "ECL reference number not found in session"
    }
  }

}