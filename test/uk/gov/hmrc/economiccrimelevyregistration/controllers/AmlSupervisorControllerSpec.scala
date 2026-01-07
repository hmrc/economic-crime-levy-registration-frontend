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
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.{AmendAmlSupervisorFormProvider, AmlSupervisorFormProvider}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{AuditError, DataRetrievalError}
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, EclRegistrationModel, NormalMode, Registration, RegistrationType}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmlSupervisorPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlSupervisorView
import uk.gov.hmrc.economiccrimelevyregistration.services.AuditService
import org.mockito.Mockito.when

import scala.concurrent.Future

class AmlSupervisorControllerSpec extends SpecBase {

  val view: AmlSupervisorView                           = app.injector.instanceOf[AmlSupervisorView]
  val formProvider: AmlSupervisorFormProvider           = new AmlSupervisorFormProvider()
  val amendFormProvider: AmendAmlSupervisorFormProvider = new AmendAmlSupervisorFormProvider()
  val form: Form[AmlSupervisor]                         = formProvider(appConfig)

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockAuditService: AuditService                     = mock[AuditService]

  implicit val arbAmlSupervisor: Arbitrary[AmlSupervisor] = arbAmlSupervisor(appConfig)

  val pageNavigator: AmlSupervisorPageNavigator = new AmlSupervisorPageNavigator {
    override protected def navigateInNormalMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = onwardRoute

    override protected def navigateInCheckMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new AmlSupervisorController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      amendFormProvider,
      appConfig,
      pageNavigator,
      view,
      mockAuditService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration) =>
        new TestContext(registration.copy(amlSupervisor = None)) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form,
            NormalMode,
            Some(RegistrationType.Initial),
            registration.carriedOutAmlRegulatedActivityInCurrentFy,
            None
          )(
            fakeRequest,
            messages
          ).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, amlSupervisor: AmlSupervisor) =>
        new TestContext(registration.copy(amlSupervisor = Some(amlSupervisor))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(amlSupervisor),
            NormalMode,
            Some(RegistrationType.Initial),
            registration.carriedOutAmlRegulatedActivityInCurrentFy,
            None
          )(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected AML supervisor option then redirect to the next page" in forAll {
      (registration: Registration, amlSupervisor: AmlSupervisor) =>
        new TestContext(registration) {
          val updatedRegistration: Registration =
            registration.copy(amlSupervisor = Some(amlSupervisor), registrationType = Some(Initial))

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))
          when(mockAuditService.sendEvent(any())(any()))
            .thenReturn(EitherT[Future, AuditError, Unit](Future.successful(Right(()))))

          val formData: Seq[(String, String)] = amlSupervisor match {
            case AmlSupervisor(Other, Some(otherProfessionalBody)) =>
              Seq(("value", Other.toString), ("otherProfessionalBody", otherProfessionalBody))
            case _                                                 => Seq(("value", amlSupervisor.supervisorType.toString))
          }

          val result: Future[Result] =
            controller.onSubmit(NormalMode, RegistrationType.Initial)(
              fakeRequest.withFormUrlEncodedBody(formData: _*)
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result]              = controller.onSubmit(NormalMode, RegistrationType.Initial)(
          fakeRequest.withFormUrlEncodedBody(("value", ""))
        )
        val formWithErrors: Form[AmlSupervisor] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(
          formWithErrors,
          NormalMode,
          Some(RegistrationType.Initial),
          registration.carriedOutAmlRegulatedActivityInCurrentFy,
          None
        )(
          fakeRequest,
          messages
        ).toString
      }
    }
  }

}
