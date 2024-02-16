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

import play.api.mvc.Call
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode}

trait DeregisterNavigator {
  def toNextPage(mode: Mode, nextPage: Call) =
    Redirect(mode match {
      case NormalMode => nextPage
      case CheckMode  => routes.DeregisterCheckYourAnswersController.onPageLoad()
    })
}