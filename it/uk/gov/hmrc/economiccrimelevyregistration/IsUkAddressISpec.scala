package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class IsUkAddressISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.IsUkAddressController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.IsUkAddressController.onPageLoad())

    "respond with 200 status and the is UK address HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.IsUkAddressController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Is your contact address based in the UK?")
    }
  }

}
