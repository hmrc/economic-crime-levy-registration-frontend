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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Other, SoleTrader}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.GrsCreateJourneyResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Mode, OtherEntityJourneyData, OtherEntityType, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.{PartnershipType, UkCompanyType}
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class OtherEntityTypePageNavigatorSpec extends SpecBase {

  val pageNavigator = new OtherEntityTypePageNavigator()

  "nextPage" should {
    "return a Call to the business sector page for all other entities" in forAll {
      (registration: Registration, journeyUrl: String, entityType: OtherEntityType, mode: Mode) =>
        val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(entityType = Some(entityType))

        val updatedRegistration: Registration = registration.copy(otherEntityJourneyData = otherEntityJourneyData)

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, routes.BusinessSectorController.onPageLoad(mode).url)
    }
  }

}
