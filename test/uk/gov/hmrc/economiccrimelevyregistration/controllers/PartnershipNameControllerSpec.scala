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

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.PartnershipNameFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.OrganisationNameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.NavigationData
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.PartnershipNamePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.PartnershipNameView

import scala.concurrent.Future

class PartnershipNameControllerSpec extends SpecBase {

  val view: PartnershipNameView                 = app.injector.instanceOf[PartnershipNameView]
  val formProvider: PartnershipNameFormProvider = new PartnershipNameFormProvider()
  val form: Form[String]                        = formProvider()

  val pageNavigator: PartnershipNamePageNavigator = new PartnershipNamePageNavigator() {
    override protected def navigateInNormalMode(navigationData: NavigationData): Call = onwardRoute
  }

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  class TestContext(registrationData: Registration) {
    val controller = new PartnershipNameController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(
        registration.copy(partnershipName = None)
      ) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, partnershipName: String) =>
        new TestContext(
          registration.copy(partnershipName = Some(partnershipName))
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(partnershipName), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided partnership name then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(OrganisationNameMaxLength)
    ) { (registration: Registration, partnershipName: String) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(partnershipName = Some(partnershipName.trim))

        when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration)))
          .thenReturn(EitherT.fromEither[Future](Right(updatedRegistration)))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", partnershipName)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]       = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
