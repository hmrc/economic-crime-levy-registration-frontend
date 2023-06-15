/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.{OK, SEE_OTHER}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{AddressLookupFrontendConnector, EclRegistrationConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.PublicBetaAction
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, OtherEntityJourneyData, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AddCTUTRView
import play.api.mvc.{BodyParsers, Call, RequestHeader, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.forms.AddCTUTRFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.arbRegistration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AddCTUTRPageNavigator

import scala.concurrent.Future

class AddCTUTRControllerSpec extends SpecBase {

  val view: AddCTUTRView                                     = app.injector.instanceOf[AddCTUTRView]
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val errorHandler: ErrorHandler                             = app.injector.instanceOf[ErrorHandler]
  val formProvider: AddCTUTRFormProvider                     = new AddCTUTRFormProvider()
  val form: Form[String]                                     = formProvider()
  val CTUTR = "0123456789"

  val pageNavigator: AddCTUTRPageNavigator = new AddCTUTRPageNavigator(mock[AddressLookupFrontendConnector]) {
    override protected def navigateInNormalMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call] = Future.successful(onwardRoute)

    override protected def navigateInCheckMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call] = Future.successful(onwardRoute)
  }

  val enabled: PublicBetaAction = new PublicBetaAction(
    errorHandler = errorHandler,
    appConfig = appConfig,
    parser = app.injector.instanceOf[BodyParsers.Default]
  )

  class TestContext(registrationData: Registration) {
    val controller = new AddCTUTRController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      enabled,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has been provided" in { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "return OK and the correct view when answer is provided" in { (registration: Registration) =>
      val otherData: OtherEntityJourneyData = registration.otherEntityJourneyData.copy(ctUtr = Some(CTUTR))
      new TestContext(registration.copy(optOtherEntityJourneyData = Some(otherData))) {

        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString()
      }
    }
  }

  "onSubmit" should {
    "redirect to the next page" in forAll(Arbitrary.arbitrary[Registration]) { (registration: Registration) =>
      val ctutr                             = CTUTR
      val otherData: OtherEntityJourneyData = registration.otherEntityJourneyData.copy(ctUtr = Some(ctutr))
      val updatedRegistration: Registration = registration.copy(optOtherEntityJourneyData = Some(otherData))
      new TestContext(updatedRegistration) {
        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", ctutr)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }
  }
}
