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

import cats.data.EitherT
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, EntityType, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, RegistrationSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.time.TaxYear

import java.time.{LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EmailService @Inject() (emailConnector: EmailConnector)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendRegistrationSubmittedEmails(
    contacts: Contacts,
    eclRegistrationReference: String,
    entityType: Option[EntityType],
    additionalInfo: Option[RegistrationAdditionalInfo],
    liableForCurrentFY: Option[Boolean]
  )(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Unit] = {
    val eclDueDate       = ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)
    val registrationDate = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)

    val previousFY =
      additionalInfo.flatMap(_.liabilityYear.flatMap(year => if (year.isNotCurrentFY) Some(year.asString) else None))
    val currentFY  =
      liableForCurrentFY.map(_ => TaxYear.current.currentYear.toString)

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
          previousFY = previousFY,
          currentFY = currentFY
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

  def sendAmendRegistrationSubmitted(
    contacts: Contacts
  )(implicit hc: HeaderCarrier, messages: Messages): EitherT[Future, DataRetrievalError, Unit] = {
    EitherT {
      (contacts.firstContactDetails.emailAddress, contacts.firstContactDetails.name) match {
        case (Some(emailAddress), Some(name)) =>
          emailConnector.sendAmendRegistrationSubmittedEmail(
            to = emailAddress,
            AmendRegistrationSubmittedEmailParameters(
              name = name,
              dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now())
            )
          )
            .map(Right(_))
            .recover {
              case error@UpstreamErrorResponse(message, code, _, _)
                if UpstreamErrorResponse.Upstream5xxResponse
                  .unapply(error)
                  .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        case _ => Left(DataRetrievalError.FieldNotFound("Invalid contact details"))
      }
    }
  }
}
