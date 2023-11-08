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
import org.scalacheck.Arbitrary
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.SoleTrader
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.GrsCreateJourneyResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Mode, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.{IncorporatedEntityType, PartnershipType}
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class EntityTypePageNavigatorSpec extends SpecBase {

  val mockIncorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector =
    mock[IncorporatedEntityIdentificationFrontendConnector]
  val mockSoleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector                 =
    mock[SoleTraderIdentificationFrontendConnector]
  val mockPartnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector               =
    mock[PartnershipIdentificationFrontendConnector]

  val pageNavigator = new EntityTypePageNavigator(
    mockIncorporatedEntityIdentificationFrontendConnector,
    mockSoleTraderIdentificationFrontendConnector,
    mockPartnershipIdentificationFrontendConnector
  )

  "nextPage" should {
    "return a Call to the incorporated entity GRS journey irrespective of mode when an incorporated entity option is selected" in forAll {
      (registration: Registration, journeyUrl: String, incorporatedEntityType: IncorporatedEntityType, mode: Mode) =>
        val entityType                        = incorporatedEntityType.entityType
        val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

        when(
          mockIncorporatedEntityIdentificationFrontendConnector
            .createIncorporatedEntityJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(
              any()
            )
        )
          .thenReturn(Future.successful(GrsCreateJourneyResponse(journeyUrl)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, journeyUrl)
    }

    "return a Call to the sole trader GRS journey irrespective of mode when the sole trader option is selected" in forAll {
      (registration: Registration, journeyUrl: String, mode: Mode) =>
        val updatedRegistration: Registration = registration.copy(entityType = Some(SoleTrader))

        when(mockSoleTraderIdentificationFrontendConnector.createSoleTraderJourney(ArgumentMatchers.eq(mode))(any()))
          .thenReturn(Future.successful(GrsCreateJourneyResponse(journeyUrl)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, journeyUrl)
    }

    "return a Call to the partnership GRS journey irrespective of mode when a partnership option is selected" in forAll {
      (registration: Registration, partnershipType: PartnershipType, journeyUrl: String, mode: Mode) =>
        val entityType: EntityType = partnershipType.entityType

        val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

        when(
          mockPartnershipIdentificationFrontendConnector
            .createPartnershipJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(
              any()
            )
        )
          .thenReturn(Future.successful(GrsCreateJourneyResponse(journeyUrl)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, journeyUrl)
    }

    "return a Call to the business name page when an other entity option is selected" in forAll(
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[EntityType].retryUntil(EntityType.isOther)
    ) { (registration: Registration, entityType: EntityType) =>
      val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

      await(pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)) shouldBe Call(
        GET,
        routes.BusinessNameController.onPageLoad(NormalMode).url
      )
    }
  }

}
