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
import uk.gov.hmrc.economiccrimelevyregistration.models.email.RegistrationSubmittedEmailParameters
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Contacts, EntityType}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val service                            = new EmailService(mockEmailConnector, appConfig)
  val entityType                         = Some(random[EntityType])

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
            service.sendRegistrationSubmittedEmails(
              updatedContacts,
              eclRegistrationReference,
              entityType
            )(hc, messages)
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
          Some(secondContactEmail)
        )

        val expectedSecondContactParams = RegistrationSubmittedEmailParameters(
          secondContactName,
          eclRegistrationReference,
          ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)(messages),
          eclDueDate,
          "false",
          Some(secondContactEmail)
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
            service.sendRegistrationSubmittedEmails(
              updatedContacts,
              eclRegistrationReference,
              entityType
            )(hc, messages)
          )

        result shouldBe ()

        verify(mockEmailConnector, times(2))
          .sendRegistrationSubmittedEmail(any(), any(), any())(any())

        reset(mockEmailConnector)
    }

    "throw an IllegalStateException when the first contact details are missing" in forAll {
      eclRegistrationReference: String =>
        when(mockEmailConnector.sendRegistrationSubmittedEmail(any(), any(), any())(any()))
          .thenReturn(Future.successful(()))

        val result = intercept[IllegalStateException] {
          await(
            service.sendRegistrationSubmittedEmails(
              Contacts.empty,
              eclRegistrationReference,
              entityType
            )(hc, messages)
          )
        }

        result.getMessage shouldBe "Invalid contact details"
    }
  }

}
