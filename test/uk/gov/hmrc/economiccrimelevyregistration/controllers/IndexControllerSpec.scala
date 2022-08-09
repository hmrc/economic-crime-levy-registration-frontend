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

import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.views.html.IndexView

class IndexControllerSpec extends SpecBase {

  val view: IndexView = app.injector.instanceOf[IndexView]

  val controller = new IndexController(
    mcc,
    fakeAuthorisedAction,
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in {
      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual view()(fakeRequest, messages).toString
    }
  }

}
