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
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, FinancialConductAuthority, GamblingCommission, Hmrc, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{FinancialConductAuthorityView, GamblingCommissionView}

import scala.concurrent.Future

class RegisterWithOtherAmlSupervisorControllerSpec extends SpecBase {

  val gcView: GamblingCommissionView         = app.injector.instanceOf[GamblingCommissionView]
  val fcaView: FinancialConductAuthorityView = app.injector.instanceOf[FinancialConductAuthorityView]

  class TestContext(registrationData: Registration) {
    val controller = new RegisterWithOtherAmlSupervisorController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      gcView,
      fcaView
    )
  }

  "onPageLoad" should {
    "return OK and the register with the Gambling Commission view when the Gambling Commission AML Supervisor option has been selected" in forAll {
      registration: Registration =>
        new TestContext(registration.copy(amlSupervisor = Some(AmlSupervisor(GamblingCommission, None)))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe gcView()(fakeRequest, messages).toString
        }
    }

    "return OK and the register with the Financial Conduct Authority view when the Financial Conduct Authority AML Supervisor option has been selected" in forAll {
      registration: Registration =>
        new TestContext(registration.copy(amlSupervisor = Some(AmlSupervisor(FinancialConductAuthority, None)))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe fcaView()(fakeRequest, messages).toString
        }
    }

    "throw an IllegalStateException when neither the Gambling Commission nor Financial Conduct Authority AML Supervisor option has been selected" in forAll {
      registration: Registration =>
        new TestContext(registration.copy(amlSupervisor = Some(AmlSupervisor(Hmrc, None)))) {
          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onPageLoad()(fakeRequest))
          }

          result.getMessage shouldBe "The AML Supervisor was not either GamblingCommission or FinancialConductAuthority as expected"
        }
    }
  }
}
