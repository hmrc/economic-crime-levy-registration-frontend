package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class FirstContactRoleISpec extends ISpecBase with AuthorisedBehaviour {

  val roleMaxLength: Int = 160

  s"GET ${contacts.routes.FirstContactRoleController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      contacts.routes.FirstContactRoleController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the first contact role HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]

      stubGetRegistration(
        registration.copy(contacts =
          registration.contacts.copy(firstContactDetails =
            registration.contacts.firstContactDetails.copy(name = Some(name))
          )
        )
      )

      val result = callRoute(FakeRequest(contacts.routes.FirstContactRoleController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's role?")
    }
  }

  s"POST ${contacts.routes.FirstContactRoleController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.FirstContactRoleController.onSubmit(NormalMode))

    "save the provided role then redirect to the first contact email page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]
      val role         = stringsWithMaxLength(roleMaxLength).sample.get

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(firstContactDetails =
          registration.contacts.firstContactDetails.copy(name = Some(name), role = Some(role))
        )
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.FirstContactRoleController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", role))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.FirstContactEmailController.onPageLoad(NormalMode).url)
    }
  }
}
