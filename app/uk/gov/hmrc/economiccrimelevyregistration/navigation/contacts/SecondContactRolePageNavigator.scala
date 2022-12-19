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

package uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.PageNavigator

class SecondContactRolePageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(registration: Registration): Call =
    registration.contacts.secondContactDetails.role match {
      case Some(_) => contacts.routes.SecondContactEmailController.onPageLoad()
      case _       => routes.StartController.onPageLoad()
    }

  override protected def navigateInCheckMode(registration: Registration): Call = ???
}
