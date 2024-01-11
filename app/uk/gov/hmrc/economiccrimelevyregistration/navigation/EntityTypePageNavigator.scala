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

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EntityType, Mode, NormalMode, Registration}
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EntityTypePageNavigator @Inject() () extends PageNavigator {

  override protected def navigateInNormalMode(
    registration: Registration,
    url: String,
    isSame: Boolean
  ): Call = navigate(registration, url, isSame, NormalMode)

  override protected def navigateInCheckMode(
    registration: Registration,
    url: String,
    isSame: Boolean
  ): Call = navigate(registration, url, isSame, CheckMode)

  private def navigate(
    registration: Registration,
    url: String,
    isSame: Boolean,
    mode: Mode
  ): Call = if ((mode == CheckMode) && isSame) {
    routes.CheckYourAnswersController.onPageLoad()
  } else {
    registration.entityType match {
      case Some(entityType) =>
        if (EntityType.isOther(entityType)) {
          routes.BusinessNameController.onPageLoad(mode)
        } else if (!url.isBlank) {
          Call(GET, url)
        } else {
          routes.NotableErrorController.answersAreInvalid()
        }
      case None             => routes.NotableErrorController.answersAreInvalid()
    }
  }
}
