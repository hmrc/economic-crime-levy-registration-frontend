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

import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.{RegistrationAdditionalInfo, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationAdditionalInfoService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}

import scala.concurrent.Future

class RegistrationSubmittedControllerSpec extends SpecBase {

  val view: RegistrationSubmittedView                                              = app.injector.instanceOf[RegistrationSubmittedView]
  val outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView =
    app.injector.instanceOf[OutOfSessionRegistrationSubmittedView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService     = mock[RegistrationAdditionalInfoService]

  val controller = new RegistrationSubmittedController(
    mcc,
    fakeAuthorisedActionWithoutEnrolmentCheck("test-internal-id"),
    view,
    outOfSessionRegistrationSubmittedView,
    mockRegistrationAdditionalInfoService
  )

  "onPageLoad" should {
    "return OK and the correct view when there is one contact email address in the session" in forAll {
      (
        eclReference: String,
        firstContactEmailAddress: String,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        when(mockRegistrationAdditionalInfoService.get(anyString())(any()))
          .thenReturn(Future.successful(Some(registrationAdditionalInfo)))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            (SessionKeys.EclReference, eclReference),
            (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress)
          )
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          eclReference,
          firstContactEmailAddress,
          None,
          registrationAdditionalInfo.liabilityYear
        )(fakeRequest, messages).toString
    }

    "return OK and the correct view when there are two contacts email address in the session" in forAll {
      (
        eclReference: String,
        firstContactEmailAddress: String,
        secondContactEmailAddress: String,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        when(mockRegistrationAdditionalInfoService.get(anyString())(any()))
          .thenReturn(Future.successful(Some(registrationAdditionalInfo)))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            (SessionKeys.EclReference, eclReference),
            (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress),
            (SessionKeys.SecondContactEmailAddress, secondContactEmailAddress)
          )
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          eclReference,
          firstContactEmailAddress,
          Some(secondContactEmailAddress),
          registrationAdditionalInfo.liabilityYear
        )(fakeRequest, messages).toString
    }

    "return OK and the correct view when information is not gathered from session" in {
      (
        internalId: String,
        groupId: String,
        eclReference: String,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        when(mockRegistrationAdditionalInfoService.get(anyString())(any()))
          .thenReturn(Future.successful(Some(registrationAdditionalInfo)))

        val result =
          controller.onPageLoad()(AuthorisedRequest(fakeRequest, internalId, groupId, Some(eclReference)))

        status(result) shouldBe OK

        contentAsString(result) shouldBe outOfSessionRegistrationSubmittedView(
          "eclReference",
          registrationAdditionalInfo.liabilityYear
        )(fakeRequest, messages)
          .toString()
    }

    "throw an IllegalStateException when the ECL reference is not found in the session or enrolment" in {
      val result: IllegalStateException = intercept[IllegalStateException] {
        await(controller.onPageLoad()(fakeRequest))
      }

      result.getMessage shouldBe "ECL reference number not found in session or in enrolment"
    }
  }

}
