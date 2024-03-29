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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, RegistrationSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, EclAddress, EntityType}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val service                            = new EmailService(mockEmailConnector)
  private val entityType                 = Some(random[EntityType])

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
          ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)(messages),
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

        val eclDueDate = ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)(messages)

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

}
