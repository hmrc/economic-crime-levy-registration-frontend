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

package uk.gov.hmrc.economiccrimelevyregistration.navigation.deregister

import play.api.http.Status.SEE_OTHER
import play.api.mvc.Call
import play.api.test.Helpers.{redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, NormalMode}
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class DeregisterNavigatorSpec extends SpecBase {
  class TestNavigator extends DeregisterNavigator

  val navigator = new TestNavigator

  val call = Call(GET, "http://some.url")

  "redirect to next page in NormalMode" in {
    val result = Future.successful(navigator.toNextPage(NormalMode, call))

    status(result)           shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(call.url)
  }

  "redirect to check your answers page in CheckMode" in {
    val result = Future.successful(navigator.toNextPage(CheckMode, call))

    status(result)           shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeregisterCheckYourAnswersController.onPageLoad().url)
  }
}
