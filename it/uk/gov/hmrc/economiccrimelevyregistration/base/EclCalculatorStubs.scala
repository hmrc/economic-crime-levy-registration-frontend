package uk.gov.hmrc.economiccrimelevyregistration.base

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CalculateLiabilityRequest, CalculatedLiability, EclAmount}

trait EclCalculatorStubs { self: WireMockStubs =>

  def stubCalculateLiability(calculateLiabilityRequest: CalculateLiabilityRequest, liable: Boolean): StubMapping = {
    val calculatedLiability = random[CalculatedLiability]

    stub(
      post(urlEqualTo("/economic-crime-levy-calculator/calculate-liability"))
        .withRequestBody(
          equalToJson(Json.toJson(calculateLiabilityRequest).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(
          Json
            .toJson(calculatedLiability.copy(amountDue = if (liable) EclAmount(amount = 1) else EclAmount(amount = 0)))
            .toString()
        )
    )
  }

}
