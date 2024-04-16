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
import uk.gov.hmrc.economiccrimelevyregistration.models.email.{AmendRegistrationSubmittedEmailParameters, DeregistrationRequestedEmailParameters, RegistrationSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, EclAddress, EntityType, GetCorrespondenceAddressDetails, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services.LocalDateService
import uk.gov.hmrc.economiccrimelevyregistration.utils.TempTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EmailService @Inject() (
  emailConnector: EmailConnector,
  localDateService: LocalDateService
)(implicit
  ec: ExecutionContext
) extends Logging {

  type ServiceResult[T] = EitherT[Future, DataRetrievalError, T]

  def sendRegistrationSubmittedEmails(
    contacts: Contacts,
    eclRegistrationReference: String,
    entityType: Option[EntityType],
    additionalInfo: Option[RegistrationAdditionalInfo],
    liableForCurrentFY: Option[Boolean]
  )(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): ServiceResult[Unit] = {
    val eclTaxYear       = TempTaxYear.fromCurrentDate(localDateService.now())
    val eclDueDate       = ViewUtils.formatLocalDate(eclTaxYear.dateDue, translate = false)
    val registrationDate = ViewUtils.formatLocalDate(LocalDate.now(ZoneOffset.UTC), translate = false)
    val previousFY       =
      additionalInfo.flatMap(_.liabilityYear.flatMap(year => if (year.isNotCurrentFY) Some(year.asString) else None))
    val currentFY        =
      liableForCurrentFY.map(_ => eclTaxYear.startYear.toString)

    def sendEmail(
      name: String,
      email: String,
      isPrimaryContact: Boolean,
      secondContactEmail: Option[String]
    ): ServiceResult[Unit] =
      EitherT {
        emailConnector
          .sendRegistrationSubmittedEmail(
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
          .map(Right(_))
          .recover {
            case error @ UpstreamErrorResponse(message, code, _, _)
                if UpstreamErrorResponse.Upstream5xxResponse
                  .unapply(error)
                  .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
              Left(DataRetrievalError.BadGateway(message, code))
            case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
          }
      }

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
      case _                                                                                                    =>
        EitherT.fromEither[Future](
          Left(DataRetrievalError.InternalUnexpectedError("Invalid contact details", None))
        )
    })

  }

  private def getContactData(
    contacts: Contacts
  ): Either[DataRetrievalError, (String, String)] =
    (contacts.firstContactDetails.emailAddress, contacts.firstContactDetails.name) match {
      case (Some(emailAddress), Some(name)) => Right((emailAddress, name))
      case _                                => Left(DataRetrievalError.InternalUnexpectedError("Invalid contact details", None))
    }

  private def sendEmail(emailAddress: String, name: String, address: Option[EclAddress])(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): ServiceResult[Unit] =
    EitherT {
      emailConnector
        .sendAmendRegistrationSubmittedEmail(
          to = emailAddress,
          AmendRegistrationSubmittedEmailParameters(
            name = name,
            dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now()),
            addressLine1 = address.flatMap(_.addressLine1),
            addressLine2 = address.flatMap(_.addressLine2),
            addressLine3 = address.flatMap(_.addressLine3),
            addressLine4 = address.flatMap(_.addressLine4),
            containsAddress = address.map(_ => "true")
          )
        )
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def sendAmendRegistrationSubmitted(
    contacts: Contacts,
    address: Option[EclAddress]
  )(implicit hc: HeaderCarrier, messages: Messages): ServiceResult[Unit] =
    for {
      data   <- EitherT.fromEither[Future](getContactData(contacts))
      result <- sendEmail(data._1, data._2, address)
    } yield result

  def sendDeregistrationEmail(
    emailAddress: String,
    name: String,
    eclReference: String,
    address: GetCorrespondenceAddressDetails
  )(implicit hc: HeaderCarrier, messages: Messages): ServiceResult[Unit] =
    EitherT {
      emailConnector
        .sendDeregistrationRequestedEmail(
          emailAddress,
          DeregistrationRequestedEmailParameters(
            name = name,
            dateSubmitted = ViewUtils.formatLocalDate(LocalDate.now()),
            eclReferenceNumber = eclReference,
            addressLine1 = Some(address.addressLine1),
            addressLine2 = address.addressLine2.map(line => line),
            addressLine3 = address.addressLine3.map(line => line),
            addressLine4 = address.addressLine4.map(line => line)
          )
        )
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }
}
