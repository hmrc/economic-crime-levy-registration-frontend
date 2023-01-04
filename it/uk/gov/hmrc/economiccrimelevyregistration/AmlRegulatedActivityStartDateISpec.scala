package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes._
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

import java.time.LocalDate

class AmlRegulatedActivityStartDateISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlRegulatedActivityStartDateController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityStartDateController.onPageLoad())

    "respond with 200 status and the Aml start date HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityStartDateController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("What date did your AML-regulated activity start?")
    }
  }

  s"POST ${routes.AmlRegulatedActivityStartDateController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityStartDateController.onSubmit())

    "save the Aml regulated activity start date then redirect to the business sector page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration                  = random[Registration]
      val amlRegulatedActivityStartDate = random[LocalDate]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(amlRegulatedActivityStartDate = Some(amlRegulatedActivityStartDate))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityStartDateController.onSubmit()).withFormUrlEncodedBody(
          ("value.day", amlRegulatedActivityStartDate.getDayOfMonth.toString),
          ("value.month", amlRegulatedActivityStartDate.getMonthValue.toString),
          ("value.year", amlRegulatedActivityStartDate.getYear.toString)
        )
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(BusinessSectorController.onPageLoad().url)
    }
  }
}
