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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.{CalculateLiabilityRequest, CalculatedLiability}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclCalculatorConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext)
    extends BaseConnector {

  def calculateLiability(relevantApLength: Int, relevantApRevenue: BigDecimal, taxYearStart: Int)(implicit
    hc: HeaderCarrier
  ): Future[CalculatedLiability] = {
    val body = CalculateLiabilityRequest(
      amlRegulatedActivityLength = EclTaxYear.yearInDays,
      relevantApLength = relevantApLength,
      ukRevenue = relevantApRevenue.toLong,
      year = taxYearStart
    )

    httpClient
      .post(url"${appConfig.eclCalculatorBaseUrl}/economic-crime-levy-calculator/calculate-liability")
      .withBody(Json.toJson(body))
      .executeAndDeserialise[CalculatedLiability]
  }
}
