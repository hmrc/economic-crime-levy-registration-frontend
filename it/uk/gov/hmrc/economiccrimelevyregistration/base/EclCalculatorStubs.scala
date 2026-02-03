package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CalculateLiabilityRequest, CalculatedLiability, EclAmount}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

trait EclCalculatorStubs { self: WireMockStubs =>

  def stubCalculateLiability(calculateLiabilityRequest: CalculateLiabilityRequest, liable: Boolean): StubMapping = {
    val calculatedLiability = arbitrary[CalculatedLiability].sample.get

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
