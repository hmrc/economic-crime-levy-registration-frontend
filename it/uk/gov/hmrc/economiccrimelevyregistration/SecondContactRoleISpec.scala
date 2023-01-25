package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactRoleISpec extends ISpecBase with AuthorisedBehaviour {

  val roleMaxLength: Int = 60

  s"GET ${contacts.routes.SecondContactRoleController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.SecondContactRoleController.onPageLoad())

    "respond with 200 status and the second contact role HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]

      stubGetRegistration(
        registration.copy(contacts =
          registration.contacts.copy(secondContactDetails =
            registration.contacts.secondContactDetails.copy(name = Some(name))
          )
        )
      )

      val result = callRoute(FakeRequest(contacts.routes.SecondContactRoleController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's role?")
    }
  }

  s"POST ${contacts.routes.SecondContactRoleController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(contacts.routes.SecondContactRoleController.onSubmit())

    "save the provided role then redirect to the second contact email page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]
      val role         = stringsWithMaxLength(roleMaxLength).sample.get

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(secondContactDetails =
          registration.contacts.secondContactDetails.copy(name = Some(name), role = Some(role))
        )
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.SecondContactRoleController.onSubmit()).withFormUrlEncodedBody(("value", role))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactEmailController.onPageLoad().url)
    }
  }
}
