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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, anyString}
import org.scalacheck.Arbitrary
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataRetrievalAction
import uk.gov.hmrc.economiccrimelevyregistration.forms.deregister.DeregisterContactNameFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.NameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.{arbDeregistration, arbMode}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterContactNameView

import scala.concurrent.Future

class DeregisterContactNameControllerSpec extends SpecBase {

  val view: DeregisterContactNameView                 = app.injector.instanceOf[DeregisterContactNameView]
  val formProvider: DeregisterContactNameFormProvider = new DeregisterContactNameFormProvider()

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]

  class TestContext(internalId: String) {
    val controller = new DeregisterContactNameController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(internalId),
      new DeregistrationDataRetrievalAction(mockDeregistrationService),
      mockDeregistrationService,
      formProvider,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { (deregistration: Deregistration, mode: Mode) =>
      new TestContext(deregistration.internalId) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

        val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

        val form = deregistration.contactDetails.name match {
          case Some(name) => formProvider().fill(name)
          case None       => formProvider()
        }

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          form,
          mode
        )(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "go to deregistration contact role view" in forAll(
      Arbitrary.arbitrary[Deregistration],
      stringsWithMaxLength(NameMaxLength)
    ) { (deregistration: Deregistration, name: String) =>
      new TestContext(deregistration.internalId) {
        when(mockDeregistrationService.getOrCreate(anyString())(any()))
          .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

        when(mockDeregistrationService.upsert(any())(any()))
          .thenReturn(
            EitherT.fromEither[Future](
              Right(deregistration.copy(contactDetails = deregistration.contactDetails.copy(name = Some(name))))
            )
          )

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> name))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DeregisterContactRoleController.onPageLoad(NormalMode).url)

        verify(mockDeregistrationService, times(1)).upsert(any())(any())
        reset(mockDeregistrationService)
      }
    }
  }
}
