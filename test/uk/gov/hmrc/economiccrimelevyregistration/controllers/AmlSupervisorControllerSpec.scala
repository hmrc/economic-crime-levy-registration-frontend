/*
 * Copyright 2022 HM Revenue & Customs
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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmlSupervisorFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlSupervisorView

import scala.concurrent.Future

class AmlSupervisorControllerSpec extends SpecBase {

  val view: AmlSupervisorView                 = app.injector.instanceOf[AmlSupervisorView]
  val formProvider: AmlSupervisorFormProvider = new AmlSupervisorFormProvider()
  val form: Form[AmlSupervisor]               = formProvider(appConfig)

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  implicit val arbAmlSupervisor: Arbitrary[AmlSupervisor] = arbAmlSupervisor(appConfig)

  class TestContext(registrationData: Registration) {
    val controller = new AmlSupervisorController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      appConfig,
      fakePageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(amlSupervisor = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, amlSupervisor: AmlSupervisor) =>
        new TestContext(registration.copy(amlSupervisor = Some(amlSupervisor))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(amlSupervisor))(fakeRequest, messages).toString
        }
    }
  }

}
