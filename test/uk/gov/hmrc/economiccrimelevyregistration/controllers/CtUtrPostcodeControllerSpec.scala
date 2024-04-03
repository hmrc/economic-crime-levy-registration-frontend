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
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.RegistrationWithUnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.forms.CtUtrPostcodeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.CtUtrPostcodeLength
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.UnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Mode, NormalMode, OtherEntityJourneyData, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.CtUtrPostcodePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CtUtrPostcodeView

import scala.concurrent.Future

class CtUtrPostcodeControllerSpec extends SpecBase {

  val view: CtUtrPostcodeView                            = app.injector.instanceOf[CtUtrPostcodeView]
  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val formProvider: CtUtrPostcodeFormProvider            = new CtUtrPostcodeFormProvider()
  val form: Form[String]                                 = formProvider()

  val pageNavigator: CtUtrPostcodePageNavigator = new CtUtrPostcodePageNavigator() {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute
    override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call  = onwardRoute
  }

  class TestContext(registration: Registration) {
    val controller = new CtUtrPostcodeController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registration.internalId),
      fakeDataRetrievalAction(registration),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in {
      (registration: Registration, mode: Mode) =>
        val updatedRegistration = registration.copy(
          entityType = Some(UnincorporatedAssociation),
          optOtherEntityJourneyData = Some(OtherEntityJourneyData.empty())
        )
        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe Ok

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString()
        }
    }

    "return OK and the correct view when answer has already been provided" in {
      (registration: Registration, mode: Mode, postcode: String) =>
        val otherEntityData     = OtherEntityJourneyData.empty().copy(postcode = Some(postcode))
        val updatedRegistration = registration.copy(
          entityType = Some(UnincorporatedAssociation),
          optOtherEntityJourneyData = Some(otherEntityData)
        )
        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe Ok

          contentAsString(result) shouldBe view(
            form.fill(updatedRegistration.otherEntityJourneyData.postcode.get),
            mode
          )(fakeRequest, messages).toString()
        }
    }
  }

  "onSubmit" should {
    "save the postcode then redirect to the next page" in forAll(
      Arbitrary.arbitrary[RegistrationWithUnincorporatedAssociation],
      stringsWithMaxLength(CtUtrPostcodeLength),
      Arbitrary.arbitrary[Mode]
    ) { (registration: RegistrationWithUnincorporatedAssociation, postcode: String, mode: Mode) =>
      val otherEntityData = registration.registration.otherEntityJourneyData.copy(postcode = Some(postcode))

      new TestContext(registration.registration) {

        val updatedRegistration: Registration =
          registration.registration.copy(optOtherEntityJourneyData = Some(otherEntityData))

        when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody("value" -> postcode))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when user has provided wrong input" in forAll(
      Arbitrary.arbitrary[Registration]
    ) { (registration: Registration) =>
      new TestContext(registration) {

        val result: Future[Result]       =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> ""))
        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString()
      }
    }
  }
}
