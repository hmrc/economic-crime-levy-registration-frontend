package uk.gov.hmrc.economiccrimelevyregistration

import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class RelevantApLengthISpec extends ISpecBase with AuthorisedBehaviour {

  val minDays = 1
  val maxDays = 999

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.RelevantApLengthController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantApLengthController.onPageLoad(mode))

      "respond with 200 status and the relevant AP length view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.RelevantApLengthController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Your relevant accounting period")
      }
    }

    s"POST ${routes.RelevantApLengthController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantApLengthController.onSubmit(mode))

      "save the relevant AP length then redirect to the UK revenue page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration     = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val relevantApLength = Gen.chooseNum[Int](minDays, maxDays).sample.get
        val additionalInfo   = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val updatedRegistration =
          registration.copy(relevantApLength = Some(relevantApLength), revenueMeetsThreshold = None)

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.RelevantApLengthController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", relevantApLength.toString))
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(mode).url)
      }
    }
  }
}
