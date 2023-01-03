package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SecondContactEmailISpec extends ISpecBase with AuthorisedBehaviour {

  val emailMaxLength: Int = 160

  s"GET ${contacts.routes.SecondContactEmailController.onPageLoad().url}" should {
    behave like authorisedActionRoute(contacts.routes.SecondContactEmailController.onPageLoad())

    "respond with 200 status and the second contact email HTML view" in {
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

      val result = callRoute(FakeRequest(contacts.routes.SecondContactEmailController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's email address?")
    }
  }

  s"POST ${contacts.routes.SecondContactEmailController.onSubmit().url}"  should {
    behave like authorisedActionRoute(contacts.routes.SecondContactEmailController.onSubmit())

    "save the provided email address then redirect to the second contact telephone number page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val name         = random[String]
      val email        = emailAddress(emailMaxLength).sample.get

      val updatedRegistration = registration.copy(contacts =
        registration.contacts.copy(secondContactDetails =
          registration.contacts.secondContactDetails.copy(name = Some(name), emailAddress = Some(email))
        )
      )

      stubGetRegistration(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(contacts.routes.SecondContactEmailController.onSubmit()).withFormUrlEncodedBody(("value", email))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.SecondContactNumberController.onPageLoad().url)
    }
  }
}
