package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes._
import uk.gov.hmrc.economiccrimelevyregistration.models._

import java.time.LocalDate

class AmlStartDateISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlStartDateController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlStartDateController.onPageLoad())

    "respond with 200 status and the Aml start date HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlStartDateController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("What date did your AML-regulated activity start?")
    }
  }

  s"POST ${routes.AmlStartDateController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.AmlStartDateController.onSubmit())

    "save the Aml start date then redirect to the SIC code page when a valid date has been provided" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val amlStartDate = random[LocalDate]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(amlStartDate = Some(amlStartDate))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlStartDateController.onSubmit()).withFormUrlEncodedBody(("value", amlStartDate.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe ???
    }
  }
}
