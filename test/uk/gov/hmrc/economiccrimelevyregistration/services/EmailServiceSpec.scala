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

package uk.gov.hmrc.economiccrimelevyregistration.services

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, DeregistrationRequestedEmailParameters, RegistrationSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, EclAddress, EntityType, GetCorrespondenceAddressDetails}
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  val mockLocalDateService: LocalDateService = mock[LocalDateService]
  val mockEmailConnector: EmailConnector     = mock[EmailConnector]

  when(mockLocalDateService.now()).thenReturn(testCurrentDate)

  val service            = new EmailService(mockEmailConnector, mockLocalDateService)
  private val entityType = Some(random[EntityType])

  "sendRegistrationSubmittedEmails" should {
    "send an email for the first contact and return unit" in forAll {
      (contacts: Contacts, firstContactName: String, firstContactEmail: String, eclRegistrationReference: String) =>
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContactDetails = ContactDetails.empty
        )

        val expectedFirstContactParams = RegistrationSubmittedEmailParameters(
          firstContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          ViewUtils.formatLocalDate(testEclTaxYear.dateDue, translate = false)(messages),
          "true",
          None,
          None,
          None
        )

        when(
          mockEmailConnector
            .sendRegistrationSubmittedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedFirstContactParams),
              ArgumentMatchers.eq(entityType)
            )(any())
        )
          .thenReturn(Future.successful(()))

        val result: Unit =
          await(
            service
              .sendRegistrationSubmittedEmails(
                updatedContacts,
                eclRegistrationReference,
                entityType,
                None,
                None
              )(hc, messages)
              .value
          )

        result shouldBe ()

        verify(mockEmailConnector, times(1))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

    "send emails for the first and second contact and return unit" in forAll {
      (
        contacts: Contacts,
        firstContactName: String,
        firstContactEmail: String,
        secondContactName: String,
        secondContactEmail: String,
        eclRegistrationReference: String
      ) =>
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContactDetails =
            contacts.secondContactDetails.copy(name = Some(secondContactName), emailAddress = Some(secondContactEmail))
        )

        val eclDueDate = ViewUtils.formatLocalDate(testEclTaxYear.dateDue, translate = false)(messages)

        val expectedFirstContactParams = RegistrationSubmittedEmailParameters(
          firstContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          eclDueDate,
          "true",
          Some(secondContactEmail),
          None,
          None
        )

        val expectedSecondContactParams = RegistrationSubmittedEmailParameters(
          secondContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          eclDueDate,
          "false",
          Some(secondContactEmail),
          None,
          None
        )

        when(
          mockEmailConnector
            .sendRegistrationSubmittedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedFirstContactParams),
              ArgumentMatchers.eq(entityType)
            )(any())
        )
          .thenReturn(Future.successful(()))

        when(
          mockEmailConnector
            .sendRegistrationSubmittedEmail(
              ArgumentMatchers.eq(secondContactEmail),
              ArgumentMatchers.eq(expectedSecondContactParams),
              ArgumentMatchers.eq(entityType)
            )(any())
        )
          .thenReturn(Future.successful(()))

        val result: Unit =
          await(
            service
              .sendRegistrationSubmittedEmails(
                updatedContacts,
                eclRegistrationReference,
                entityType,
                None,
                None
              )(hc, messages)
              .value
          )

        result shouldBe ()

        verify(mockEmailConnector, times(2))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

    "return an error when the email connector returns an Upstream4xxResponse" in forAll {
      (contacts: Contacts, firstContactName: String, firstContactEmail: String, eclRegistrationReference: String) =>
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContactDetails = ContactDetails.empty
        )

        val expectedFirstContactParams = RegistrationSubmittedEmailParameters(
          firstContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          ViewUtils.formatLocalDate(testEclTaxYear.dateDue, translate = false)(messages),
          "true",
          None,
          None,
          None
        )

        when(
          mockEmailConnector
            .sendRegistrationSubmittedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedFirstContactParams),
              ArgumentMatchers.eq(entityType)
            )(any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Unable to send email", BAD_REQUEST)))

        val result =
          await(
            service
              .sendRegistrationSubmittedEmails(
                updatedContacts,
                eclRegistrationReference,
                entityType,
                None,
                None
              )(hc, messages)
              .value
          )

        result shouldBe Left(DataRetrievalError.BadGateway("Unable to send email", BAD_REQUEST))

        verify(mockEmailConnector, times(1))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

    "return an error when the email connector returns an Upstream5xxResponse" in forAll {
      (contacts: Contacts, firstContactName: String, firstContactEmail: String, eclRegistrationReference: String) =>
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContactDetails = ContactDetails.empty
        )

        val expectedFirstContactParams = RegistrationSubmittedEmailParameters(
          firstContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          ViewUtils.formatLocalDate(testEclTaxYear.dateDue, translate = false)(messages),
          "true",
          None,
          None,
          None
        )

        when(
          mockEmailConnector
            .sendRegistrationSubmittedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedFirstContactParams),
              ArgumentMatchers.eq(entityType)
            )(any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Unable to send email", INTERNAL_SERVER_ERROR)))

        val result =
          await(
            service
              .sendRegistrationSubmittedEmails(
                updatedContacts,
                eclRegistrationReference,
                entityType,
                None,
                None
              )(hc, messages)
              .value
          )

        result shouldBe Left(DataRetrievalError.BadGateway("Unable to send email", INTERNAL_SERVER_ERROR))

        verify(mockEmailConnector, times(1))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

    "return an InternalUnexpectedError when the email connector returns an exception" in forAll {
      (contacts: Contacts, firstContactName: String, firstContactEmail: String, eclRegistrationReference: String) =>
        val exception       = new NullPointerException("Null pointer exception")
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail)),
          secondContactDetails = ContactDetails.empty
        )

        val expectedFirstContactParams = RegistrationSubmittedEmailParameters(
          firstContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          ViewUtils.formatLocalDate(testEclTaxYear.dateDue, translate = false)(messages),
          "true",
          None,
          None,
          None
        )

        when(
          mockEmailConnector
            .sendRegistrationSubmittedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedFirstContactParams),
              ArgumentMatchers.eq(entityType)
            )(any())
        )
          .thenReturn(Future.failed(exception))

        val result =
          await(
            service
              .sendRegistrationSubmittedEmails(
                updatedContacts,
                eclRegistrationReference,
                entityType,
                None,
                None
              )(hc, messages)
              .value
          )

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))

        verify(mockEmailConnector, times(1))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

    "return an InternalUnexpectedError when there are no valid contact details" in forAll {
      (contacts: Contacts, eclRegistrationReference: String) =>
        val updatedContacts = contacts.copy(
          firstContactDetails = ContactDetails.empty,
          secondContactDetails = ContactDetails.empty
        )

        val result =
          await(
            service
              .sendRegistrationSubmittedEmails(
                updatedContacts,
                eclRegistrationReference,
                entityType,
                None,
                None
              )(hc, messages)
              .value
          )

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError("Invalid contact details", None))

        verify(mockEmailConnector, times(0))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

  }

  "sendAmendRegistrationSubmittedEmail" should {
    "send an email when an address is present and return unit" in forAll {
      (contacts: Contacts, firstContactName: String, firstContactEmail: String, eclAddress: EclAddress) =>
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail))
        )
        val date            = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages)

        val expectedParams = AmendRegistrationSubmittedEmailParameters(
          firstContactName,
          date,
          eclAddress.addressLine1,
          eclAddress.addressLine2,
          eclAddress.addressLine3,
          eclAddress.addressLine4,
          Some("true")
        )

        when(
          mockEmailConnector.sendAmendRegistrationSubmittedEmail(
            ArgumentMatchers.eq(firstContactEmail),
            ArgumentMatchers.eq(expectedParams)
          )(any())
        ).thenReturn(Future.successful(()))

        val result: Unit =
          await(service.sendAmendRegistrationSubmitted(updatedContacts, Some(eclAddress))(hc, messages).value)

        result shouldBe ()

        verify(mockEmailConnector, times(1))
          .sendAmendRegistrationSubmittedEmail(any(), any())(any())

        reset(mockEmailConnector)
    }

    "send an email when an address is NOT present and return unit" in forAll {
      (contacts: Contacts, firstContactName: String, firstContactEmail: String) =>
        val updatedContacts = contacts.copy(
          firstContactDetails =
            contacts.firstContactDetails.copy(name = Some(firstContactName), emailAddress = Some(firstContactEmail))
        )
        val date            = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages)

        val expectedParams = AmendRegistrationSubmittedEmailParameters(
          firstContactName,
          date,
          None,
          None,
          None,
          None,
          None
        )

        when(
          mockEmailConnector.sendAmendRegistrationSubmittedEmail(
            ArgumentMatchers.eq(firstContactEmail),
            ArgumentMatchers.eq(expectedParams)
          )(any())
        ).thenReturn(Future.successful(()))

        val result: Unit = await(service.sendAmendRegistrationSubmitted(updatedContacts, None)(hc, messages).value)

        result shouldBe ()

        verify(mockEmailConnector, times(1))
          .sendAmendRegistrationSubmittedEmail(any(), any())(any())

        reset(mockEmailConnector)
    }
  }

  "sendDeregistrationSubmittedEmails" should {
    "send an email and return unit" in forAll {
      (
        firstContactName: String,
        firstContactEmail: String,
        address: GetCorrespondenceAddressDetails
      ) =>
        val dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now())(messages)

        val expectedParameters = DeregistrationRequestedEmailParameters(
          firstContactName,
          dateSubmitted,
          testEclRegistrationReference,
          Some(address.addressLine1),
          address.addressLine2,
          address.addressLine3,
          address.addressLine4
        )

        when(
          mockEmailConnector
            .sendDeregistrationRequestedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedParameters)
            )(any())
        )
          .thenReturn(Future.successful(()))

        val result: Unit =
          await(
            service
              .sendDeregistrationEmail(
                firstContactEmail,
                firstContactName,
                testEclRegistrationReference,
                address
              )(hc, messages)
              .value
          )

        result shouldBe ()

        verify(mockEmailConnector, times(1))
          .sendDeregistrationRequestedEmail(any(), any())(any())

        reset(mockEmailConnector)
    }

    "return an error when the email connector returns an Upstream5xxResponse" in forAll {
      (
        firstContactName: String,
        firstContactEmail: String,
        address: GetCorrespondenceAddressDetails
      ) =>
        val dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now())(messages)

        val expectedParameters = DeregistrationRequestedEmailParameters(
          firstContactName,
          dateSubmitted,
          testEclRegistrationReference,
          Some(address.addressLine1),
          address.addressLine2,
          address.addressLine3,
          address.addressLine4
        )

        when(
          mockEmailConnector
            .sendDeregistrationRequestedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedParameters)
            )(any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Unable to send email", INTERNAL_SERVER_ERROR)))

        val result = await(
          service
            .sendDeregistrationEmail(
              firstContactEmail,
              firstContactName,
              testEclRegistrationReference,
              address
            )(hc, messages)
            .value
        )

        result shouldBe Left(DataRetrievalError.BadGateway("Unable to send email", INTERNAL_SERVER_ERROR))

        reset(mockEmailConnector)
    }

    "return an error when the email connector returns an Upstream4xxResponse" in forAll {
      (
        firstContactName: String,
        firstContactEmail: String,
        address: GetCorrespondenceAddressDetails
      ) =>
        val dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now())(messages)

        val expectedParameters = DeregistrationRequestedEmailParameters(
          firstContactName,
          dateSubmitted,
          testEclRegistrationReference,
          Some(address.addressLine1),
          address.addressLine2,
          address.addressLine3,
          address.addressLine4
        )

        when(
          mockEmailConnector
            .sendDeregistrationRequestedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedParameters)
            )(any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Unable to send email", BAD_REQUEST)))

        val result = await(
          service
            .sendDeregistrationEmail(
              firstContactEmail,
              firstContactName,
              testEclRegistrationReference,
              address
            )(hc, messages)
            .value
        )

        result shouldBe Left(DataRetrievalError.BadGateway("Unable to send email", BAD_REQUEST))

        reset(mockEmailConnector)
    }

    "return an InternalUnexpectedError when the email connector throws a non fatal exception" in forAll {
      (
        firstContactName: String,
        firstContactEmail: String,
        address: GetCorrespondenceAddressDetails
      ) =>
        val exception          = new NullPointerException("Null pointer exception")
        val dateSubmitted      = ViewUtils.formatLocalDate(LocalDate.now())(messages)
        val expectedParameters = DeregistrationRequestedEmailParameters(
          firstContactName,
          dateSubmitted,
          testEclRegistrationReference,
          Some(address.addressLine1),
          address.addressLine2,
          address.addressLine3,
          address.addressLine4
        )

        when(
          mockEmailConnector
            .sendDeregistrationRequestedEmail(
              ArgumentMatchers.eq(firstContactEmail),
              ArgumentMatchers.eq(expectedParameters)
            )(any())
        )
          .thenReturn(Future.failed(exception))

        val result = await(
          service
            .sendDeregistrationEmail(
              firstContactEmail,
              firstContactName,
              testEclRegistrationReference,
              address
            )(hc, messages)
            .value
        )

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))

        reset(mockEmailConnector)
    }
  }
}
