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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, IncorporatedEntityCreateJourneyRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncorporatedEntityIdentificationFrontendConnector @Inject() (
  val messagesApi: MessagesApi,
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  ec: ExecutionContext
) extends I18nSupport {
  private val limitedCompanyJourneyUrl = s"${appConfig.incorporatedEntityIdentificationApiUrl}/limited-company-journey"
  private val createJourneyRequest     = IncorporatedEntityCreateJourneyRequest(
    continueUrl = "",
    optServiceName = None,
    deskProServiceId = appConfig.appName,
    signOutUrl = routes.SignOutController.signOut().url,
    accessibilityUrl = ""
  )

  def createLimitedCompanyJourney()(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[GrsCreateJourneyResponse] =
    httpClient.POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      limitedCompanyJourneyUrl,
      createJourneyRequest.copy(optServiceName = Some(request2Messages(request)("service.name")))
    )
}
