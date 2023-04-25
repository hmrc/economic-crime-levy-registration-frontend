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

import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RegistrationSubmittedView

import scala.concurrent.Future

class RegistrationSubmittedControllerSpec extends SpecBase {

  val view: RegistrationSubmittedView = app.injector.instanceOf[RegistrationSubmittedView]

  val controller = new RegistrationSubmittedController(
    mcc,
    fakeAuthorisedActionWithoutEnrolmentCheck("test-internal-id"),
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (
        eclReference: String,
        firstContactEmailAddress: String,
        secondContact: Boolean,
        secondContactEmailAddress: String
      ) =>
        val result: Future[Result] =
          controller.onPageLoad()(
            fakeRequest.withSession(
              (SessionKeys.EclReference, eclReference),
              (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress),
              (SessionKeys.SecondContact, secondContact.toString),
              (SessionKeys.SecondContactEmailAddress, secondContactEmailAddress)
            )
          )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          eclReference,
          firstContactEmailAddress,
          secondContact,
          secondContactEmailAddress
        )(fakeRequest, messages).toString
    }

    "throw an IllegalStateException when the ECL reference is not found in the session" in {
      val result: IllegalStateException = intercept[IllegalStateException] {
        await(controller.onPageLoad()(fakeRequest))
      }

      result.getMessage shouldBe "ECL reference number not found in session"
    }

    "throw an IllegalStateException when the first contact email address is not found in the session" in forAll {
      eclReference: String =>
        val result: IllegalStateException = intercept[IllegalStateException] {
          await(controller.onPageLoad()(fakeRequest.withSession((SessionKeys.EclReference, eclReference))))
        }

        result.getMessage shouldBe "First contact email address not found in session"
    }

    "throw an IllegalStateException when the second contact is not found in the session" in forAll {
      (eclReference: String, firstContactEmailAddress: String) =>
        val result: IllegalStateException = intercept[IllegalStateException] {
          await(
            controller.onPageLoad()(
              fakeRequest.withSession(
                (SessionKeys.EclReference, eclReference),
                (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress)
              )
            )
          )
        }

        result.getMessage shouldBe "Second contact not found in session"
    }

    "throw an IllegalStateException when the second contact email address is not found in the session" in forAll {
      (eclReference: String, firstContactEmailAddress: String, secondContact: Boolean) =>
        val result: IllegalStateException = intercept[IllegalStateException] {
          await(
            controller.onPageLoad()(
              fakeRequest.withSession(
                (SessionKeys.EclReference, eclReference),
                (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress),
                (SessionKeys.SecondContact, secondContact.toString)
              )
            )
          )
        }

        result.getMessage shouldBe "Second contact email address not found in session"
    }
  }

}
