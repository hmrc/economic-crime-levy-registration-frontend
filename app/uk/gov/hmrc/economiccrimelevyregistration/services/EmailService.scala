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

import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, EntityType, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, RegistrationSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject() (emailConnector: EmailConnector, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendRegistrationSubmittedEmails(
    contacts: Contacts,
    eclRegistrationReference: String,
    entityType: Option[EntityType],
    additionalInfo: Option[RegistrationAdditionalInfo]
  )(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Unit] = {
    val eclDueDate       = ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)
    val registrationDate = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)

    def sendEmail(
      name: String,
      email: String,
      isPrimaryContact: Boolean,
      secondContactEmail: Option[String]
    ): Future[Unit] =
      emailConnector.sendRegistrationSubmittedEmail(
        email,
        RegistrationSubmittedEmailParameters(
          name = name,
          eclRegistrationReference = eclRegistrationReference,
          eclRegistrationDate = registrationDate,
          eclDueDate,
          isPrimaryContact = isPrimaryContact.toString,
          secondContactEmail = secondContactEmail,
          previousFY = ???,
          currentFY = ???
        ),
        entityType
      )

    ((
      contacts.firstContactDetails.name,
      contacts.firstContactDetails.emailAddress,
      contacts.secondContactDetails.name,
      contacts.secondContactDetails.emailAddress
    ) match {
      case (Some(firstContactName), Some(firstContactEmail), Some(secondContactName), Some(secondContactEmail)) =>
        for {
          _ <- sendEmail(
                 name = firstContactName,
                 email = firstContactEmail,
                 isPrimaryContact = true,
                 secondContactEmail = Some(secondContactEmail)
               )
          _ <- sendEmail(
                 name = secondContactName,
                 email = secondContactEmail,
                 isPrimaryContact = false,
                 secondContactEmail = Some(secondContactEmail)
               )
        } yield ()
      case (Some(firstContactName), Some(firstContactEmail), None, None)                                        =>
        sendEmail(
          name = firstContactName,
          email = firstContactEmail,
          isPrimaryContact = true,
          secondContactEmail = None
        )
      case _                                                                                                    => throw new IllegalStateException("Invalid contact details")
    }).recover { case e: Throwable =>
      logger.error(s"Failed to send email: ${e.getMessage}")
      throw e
    }

  }

  def sendAmendRegistrationSubmitted(contacts: Contacts)(implicit hc: HeaderCarrier, messages: Messages): Future[Unit] =
    (contacts.firstContactDetails.emailAddress, contacts.firstContactDetails.name) match {
      case (Some(emailAddress), Some(name)) =>
        emailConnector.sendAmendRegistrationSubmittedEmail(
          to = emailAddress,
          AmendRegistrationSubmittedEmailParameters(
            name = name,
            dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now())
          )
        )
      case _                                => throw new IllegalStateException("Invalid contact details")
    }
}
