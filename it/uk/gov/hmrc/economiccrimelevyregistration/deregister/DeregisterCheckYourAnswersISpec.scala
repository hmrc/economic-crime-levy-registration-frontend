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

package uk.gov.hmrc.economiccrimelevyregistration.deregister

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.DeregisterPdfEncoder
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes._
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, GetSubscriptionResponse, Languages}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregistrationPdfView
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{DeregistrationRequestedEmailParameters, DeregistrationRequestedEmailRequest}
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import java.time.{LocalDate, ZoneOffset}

class DeregisterCheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour with DeregisterPdfEncoder {

  s"GET ${DeregisterCheckYourAnswersController.onPageLoad().url}"  should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      DeregisterCheckYourAnswersController
        .onPageLoad()
    )

    "respond with 200 status and the deregister name HTML view" in {
      stubAuthorisedWithEclEnrolment()
      val deregistration = random[Deregistration].copy(internalId = testInternalId)
      stubGetDeregistration(deregistration)
      stubUpsertDeregistration(
        deregistration.copy(eclReference = Some(testEclRegistrationReference))
      )
      stubGetSubscription(random[GetSubscriptionResponse])

      val result = callRoute(
        FakeRequest(
          DeregisterCheckYourAnswersController
            .onPageLoad()
        )
      )

      status(result) shouldBe OK
      html(result)     should include(s"Check your answers")
    }
  }

  s"POST ${DeregisterCheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      DeregisterCheckYourAnswersController
        .onSubmit()
    )

    "send email and redirect the deregistration requested page" in {
      stubAuthorisedWithEclEnrolment()

      val email = alphaNumericString
      val name  = alphaNumericString
      val date  = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(
        messagesApi.preferred(Seq(Languages.english))
      )

      val subscription = random[GetSubscriptionResponse]
      val emailParams  = random[DeregistrationRequestedEmailParameters]
        .copy(
          name = name,
          dateSubmitted = date,
          eclReferenceNumber = testEclRegistrationReference,
          addressLine1 = Some(subscription.correspondenceAddressDetails.addressLine1),
          addressLine2 = subscription.correspondenceAddressDetails.addressLine2,
          addressLine3 = subscription.correspondenceAddressDetails.addressLine3,
          addressLine4 = subscription.correspondenceAddressDetails.addressLine4
        )

      val deregistration = random[Deregistration].copy(
        internalId = testInternalId,
        eclReference = Some(testEclRegistrationReference),
        contactDetails = ContactDetails(Some(name), None, Some(email), None)
      )

      stubGetDeregistration(deregistration)

      val emailRequest = DeregistrationRequestedEmailRequest(Seq(email), parameters = emailParams)

      stubGetSubscription(subscription)
      stubSendDeregistrationRequestedEmail(emailRequest)

      val pdfView: DeregistrationPdfView = app.injector.instanceOf[DeregistrationPdfView]

      val fakeRequest = FakeRequest(
        DeregisterCheckYourAnswersController
          .onSubmit()
      )

      val messages: Messages = messagesApi.preferred(fakeRequest)

      val deregisterHtmlView =
        createPdfView(subscription, pdfView, Some(testEclRegistrationReference), deregistration)(
          messages
        )

      val base64EncodedHtmlViewForPdf = base64EncodeHtmlView(deregisterHtmlView.toString())

      val updatedDeregistration =
        deregistration
          .copy(
            dmsSubmissionHtml = Some(base64EncodedHtmlViewForPdf)
          )

      stubUpsertDeregistration(updatedDeregistration)

      stubSubmitDeregistration()

      val result = callRoute(fakeRequest)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        DeregistrationRequestedController
          .onPageLoad()
          .url
      )
    }
  }

}
