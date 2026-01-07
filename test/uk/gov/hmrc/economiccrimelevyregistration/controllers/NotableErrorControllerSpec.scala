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

import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.views.html._
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.AlreadyDeregisteredView
import uk.gov.hmrc.economiccrimelevyregistration.{IncorporatedEntityType, SelfAssessmentEntityType}

import scala.concurrent.Future

class NotableErrorControllerSpec extends SpecBase {

  val answersAreInvalidView: AnswersAreInvalidView                         = app.injector.instanceOf[AnswersAreInvalidView]
  val userAlreadyEnrolledView: UserAlreadyEnrolledView                     = app.injector.instanceOf[UserAlreadyEnrolledView]
  val groupAlreadyEnrolledView: GroupAlreadyEnrolledView                   = app.injector.instanceOf[GroupAlreadyEnrolledView]
  val agentCannotRegisterView: AgentCannotRegisterView                     = app.injector.instanceOf[AgentCannotRegisterView]
  val assistantCannotRegisterView: AssistantCannotRegisterView             = app.injector.instanceOf[AssistantCannotRegisterView]
  val organisationAlreadyRegisteredView: OrganisationAlreadyRegisteredView =
    app.injector.instanceOf[OrganisationAlreadyRegisteredView]
  val registrationFailedView: RegistrationFailedView                       = app.injector.instanceOf[RegistrationFailedView]
  val partyTypeMismatchView: PartyTypeMismatchView                         = app.injector.instanceOf[PartyTypeMismatchView]
  val verfificationFailedView: VerfificationFailedView                     = app.injector.instanceOf[VerfificationFailedView]

  val youHaveAlreadyRegisteredView: YouHaveAlreadyRegisteredView = app.injector.instanceOf[YouHaveAlreadyRegisteredView]

  val youAlreadyRequestedToAmendView: YouAlreadyRequestedToAmendView =
    app.injector.instanceOf[YouAlreadyRequestedToAmendView]

  val alreadyDeregisteredView: AlreadyDeregisteredView = app.injector.instanceOf[AlreadyDeregisteredView]

  class TestContext(registrationData: Registration, eclRegistrationReference: Option[String] = None) {
    val controller = new NotableErrorController(
      mcc,
      fakeAuthorisedActionWithoutEnrolmentCheck(registrationData.internalId, eclRegistrationReference),
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeAuthorisedActionAgentsAllowed,
      fakeAuthorisedActionAssistantsAllowed,
      fakeRegistrationDataAction(registrationData),
      appConfig,
      userAlreadyEnrolledView,
      groupAlreadyEnrolledView,
      answersAreInvalidView,
      agentCannotRegisterView,
      assistantCannotRegisterView,
      organisationAlreadyRegisteredView,
      registrationFailedView,
      partyTypeMismatchView,
      verfificationFailedView,
      youHaveAlreadyRegisteredView,
      youAlreadyRequestedToAmendView,
      alreadyDeregisteredView
    )
  }

  "answerAreInvalid" should {
    "return OK and the correct view" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result] = controller.answersAreInvalid()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe answersAreInvalidView()(fakeRequest, messages).toString
      }
    }
  }

  "userAlreadyEnrolled" should {
    "return OK and the correct view" in forAll { (registration: Registration, eclRegistrationReference: String) =>
      new TestContext(registration, Some(eclRegistrationReference)) {
        val result: Future[Result] = controller.userAlreadyEnrolled()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe userAlreadyEnrolledView(eclRegistrationReference)(
          fakeRequest,
          messages
        ).toString
      }
    }
  }

  "groupAlreadyEnrolled" should {
    "return OK and the correct view" in forAll { (registration: Registration, eclRegistrationReference: String) =>
      new TestContext(registration, Some(eclRegistrationReference)) {
        val result: Future[Result]    = controller.groupAlreadyEnrolled()(fakeRequest)
        val taxAndSchemeManagementUrl =
          s"${appConfig.taxAndSchemeManagementUrl}/services/${EclEnrolment.serviceName}/${EclEnrolment.identifierKey}~$eclRegistrationReference/users"

        status(result) shouldBe OK

        contentAsString(result) shouldBe groupAlreadyEnrolledView(eclRegistrationReference, taxAndSchemeManagementUrl)(
          fakeRequest,
          messages
        ).toString
      }
    }
  }

  "agentCannotRegister" should {
    "return OK and the correct view" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result] = controller.agentCannotRegister()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe agentCannotRegisterView()(fakeRequest, messages).toString
      }
    }
  }

  "assistantCannotRegister" should {
    "return OK and the correct view" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result] = controller.assistantCannotRegister()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe assistantCannotRegisterView()(fakeRequest, messages).toString
      }
    }
  }

  "organisationAlreadyRegistered" should {
    "return OK and the correct view" in forAll { (registration: Registration, eclRegistrationReference: String) =>
      new TestContext(registration, Some(eclRegistrationReference)) {
        val result: Future[Result] = controller.organisationAlreadyRegistered(eclRegistrationReference)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe organisationAlreadyRegisteredView(eclRegistrationReference)(
          fakeRequest,
          messages
        ).toString
      }
    }
  }

  "registrationFailed" should {
    "return OK and the correct view" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/registration-failed")
        val result: Future[Result]                       = controller.registrationFailed(request)

        status(result) shouldBe OK

        contentAsString(result) shouldBe registrationFailedView()(request, messages).toString
      }
    }
  }

  "partyTypeMismatch" should {
    "return OK and the correct view" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result] = controller.partyTypeMismatch()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe partyTypeMismatchView()(fakeRequest, messages).toString
      }
    }
  }

  "verificationFailed" should {
    "return OK and the correct view for an incorporated entity" in forAll {
      (registration: Registration, incorporatedEntityType: IncorporatedEntityType) =>
        new TestContext(registration.copy(entityType = Some(incorporatedEntityType.entityType))) {
          val result: Future[Result] = controller.verificationFailed()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe verfificationFailedView()(
            fakeRequest,
            messages
          ).toString
        }
    }

    "return OK and the correct view for a partnership or sole trader" in forAll {
      (registration: Registration, selfAssessmentEntityType: SelfAssessmentEntityType) =>
        new TestContext(registration.copy(entityType = Some(selfAssessmentEntityType.entityType))) {
          val result: Future[Result] = controller.verificationFailed()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe verfificationFailedView()(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "youHaveAlreadyRegistered" should {
    "return OK and the correct view" in forAll { (registration: Registration, eclRegistrationReference: String) =>
      new TestContext(registration, Some(eclRegistrationReference)) {
        val result: Future[Result] = controller.youHaveAlreadyRegistered()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe youHaveAlreadyRegisteredView()(fakeRequest, messages).toString
      }
    }
  }

  "youAlreadyRequestedToAmend" should {
    "return OK and the correct view" in forAll { (registration: Registration, eclRegistrationReference: String) =>
      new TestContext(registration, Some(eclRegistrationReference)) {
        val result: Future[Result] = controller.youAlreadyRequestedToAmend()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe youAlreadyRequestedToAmendView()(fakeRequest, messages).toString
      }
    }
  }

  "alreadyDeregistered" should {
    "return OK and the correct view" in forAll { (registration: Registration, eclRegistrationReference: String) =>
      new TestContext(registration, Some(eclRegistrationReference)) {
        val result: Future[Result] = controller.alreadyDeregistered()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe alreadyDeregisteredView()(fakeRequest, messages).toString
      }
    }
  }

}
