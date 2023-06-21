package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.RegistrationWithUnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, Registration}
import uk.gov.hmrc.http.HttpVerbs.GET

class CtUtrPostcodePageNavigatorSpec extends SpecBase{

  val pageNavigator = new CtUtrPostcodePageNavigator()

  "nextPage" should {
    "return a call to the address lookup journey in either mode" in {
      (registration: RegistrationWithUnincorporatedAssociation, journeyUrl: String, mode: Mode) =>
        val updatedRegistration: Registration = registration.registration
        pageNavigator.nextPage(mode, updatedRegistration) shouldBe Call(GET, journeyUrl)
    }
  }

}
