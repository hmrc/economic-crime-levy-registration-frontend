package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class AmlSupervisorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlSupervisorController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AmlSupervisorController.onPageLoad(NormalMode))

    "respond with 200 status and the AML supervisor HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.AmlSupervisorController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"Who is your AML supervisor?")
    }
  }

  s"POST ${routes.AmlSupervisorController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AmlSupervisorController.onPageLoad(NormalMode))

    "save the selected option then redirect to the relevant AP 12 months page when the answer is either HMRC or another professional body" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
      val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample.get
      val amlSupervisor         =
        AmlSupervisor(supervisorType, if (supervisorType == Hmrc) None else Some(otherProfessionalBody))

      val formData: Seq[(String, String)] = amlSupervisor match {
        case AmlSupervisor(Other, Some(otherProfessionalBody)) =>
          Seq(("value", Other.toString), ("otherProfessionalBody", otherProfessionalBody))
        case _                                                 => Seq(("value", amlSupervisor.supervisorType.toString))
      }

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(amlSupervisor = Some(amlSupervisor))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.AmlSupervisorController.onSubmit(NormalMode)).withFormUrlEncodedBody(formData: _*)
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url)
    }
  }

}
