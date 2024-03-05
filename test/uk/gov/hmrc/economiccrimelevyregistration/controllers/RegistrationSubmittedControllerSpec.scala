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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}
import scala.concurrent.Future

class RegistrationSubmittedControllerSpec extends SpecBase {

  val view: RegistrationSubmittedView                                              = app.injector.instanceOf[RegistrationSubmittedView]
  val outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView =
    app.injector.instanceOf[OutOfSessionRegistrationSubmittedView]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService     = mock[RegistrationAdditionalInfoService]
  val mockEclRegistrationService: EclRegistrationService                           = mock[EclRegistrationService]

  class TestContext(registrationData: Registration, additionalInfo: Option[RegistrationAdditionalInfo] = None) {
    val controller = new RegistrationSubmittedController(
      mcc,
      fakeAuthorisedActionWithoutEnrolmentCheck(testInternalId),
      fakeDataRetrievalAction(registrationData, additionalInfo),
      view,
      outOfSessionRegistrationSubmittedView,
      mockRegistrationAdditionalInfoService,
      mockEclRegistrationService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when there is one contact email address and aml activity in the session" in forAll {
      (
        liabilityYear: LiabilityYear,
        firstContactEmailAddress: String,
        secondContactEmailAddress: Option[String],
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo
      ) =>
        val contacts              = registration.contacts.copy(
          firstContactDetails =
            registration.contacts.firstContactDetails.copy(emailAddress = Some(firstContactEmailAddress)),
          secondContactDetails =
            registration.contacts.secondContactDetails.copy(emailAddress = secondContactEmailAddress)
        )
        val updatedRegistration   = registration.copy(contacts = contacts)
        val updatedAdditionalInfo =
          additionalInfo.copy(liabilityYear = Some(liabilityYear), eclReference = Some(testEclRegistrationReference))
        new TestContext(updatedRegistration, Some(updatedAdditionalInfo)) {
          when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest
          )

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            testEclRegistrationReference,
            firstContactEmailAddress,
            secondContactEmailAddress,
            updatedAdditionalInfo.liabilityYear
          )(fakeRequest, messages).toString
        }
    }

  }

}
