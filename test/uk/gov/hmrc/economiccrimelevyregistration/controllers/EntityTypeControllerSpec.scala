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
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.EntityTypeDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{AuditError, DataRetrievalError}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.EntityTypeView

import scala.concurrent.Future

class EntityTypeControllerSpec extends SpecBase {

  val view: EntityTypeView                 = app.injector.instanceOf[EntityTypeView]
  val formProvider: EntityTypeFormProvider = new EntityTypeFormProvider()
  val form: Form[EntityType]               = formProvider()

  val dataCleanup: EntityTypeDataCleanup = new EntityTypeDataCleanup {
    override def cleanup(registration: Registration): Registration                = registration
    override def cleanupOtherEntityData(registration: Registration): Registration = registration
  }

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockAuditConnector: AuditService                   = mock[AuditService]

  class TestContext(registrationData: Registration) {
    val controller = new EntityTypeController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      dataCleanup,
      mockAuditConnector,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration.copy(entityType = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, entityType: EntityType, mode: Mode) =>
        new TestContext(registration.copy(entityType = Some(entityType))) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(entityType), mode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected entity type then redirect to the next page" in forAll {
      (randomReg: Registration, entityType: EntityType, url: String, mode: Mode) =>
        val registration = mode match {
          case NormalMode => randomReg.copy(entityType = None)
          case CheckMode  => randomReg.copy(entityType = Some(random[EntityType]))
        }

        new TestContext(registration) {
          val updatedRegistration: Registration = registration.copy(
            entityType = Some(entityType)
          )

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockAuditConnector.sendEvent(any())(any()))
            .thenReturn(EitherT[Future, AuditError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.registerEntityType(any(), any())(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, String](Future.successful(Right(url))))

          val nextPage: Call = if (registration.entityType.contains(entityType)) {
            routes.CheckYourAnswersController.onPageLoad()
          } else if (EntityType.isOther(entityType)) {
            routes.BusinessNameController.onPageLoad(mode)
          } else {
            Call(GET, url)
          }

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", entityType.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(nextPage.url)

        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration) {
          val result: Future[Result]           = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[EntityType] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }
  }
}
