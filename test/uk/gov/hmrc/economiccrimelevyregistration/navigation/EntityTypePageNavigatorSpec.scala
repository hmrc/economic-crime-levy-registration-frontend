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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, Registration}
import uk.gov.hmrc.http.HttpVerbs.GET

class EntityTypePageNavigatorSpec extends SpecBase {

  val pageNavigator = new EntityTypePageNavigator()

  "nextPage" should {
    "return a Call to the GRS journey irrespective of mode" in forAll {
      (registration: Registration, journeyUrl: String, incorporatedEntityType: IncorporatedEntityType, mode: Mode) =>
        val entityType                        = incorporatedEntityType.entityType
        val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

        pageNavigator.nextPage(mode, NavigationData(updatedRegistration, journeyUrl)) shouldBe
          Call(GET, journeyUrl)
    }
  }

}
