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
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial

@Singleton
class EclRegistrationService @Inject() (
  eclRegistrationConnector: EclRegistrationConnector,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector,
  partnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector,
  auditService: AuditService,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {

  def getOrCreate(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, Registration] =
    get(internalId).flatMap {
      case Some(registration) =>
        EitherT[Future, DataRetrievalError, Registration](Future.successful(Right(registration)))
      case None               =>
        auditService.sendRegistrationStartedEvent(internalId)

        val registration = Registration.empty(internalId).copy(registrationType = Some(Initial))
        upsertRegistration(registration).map(_ => registration)
    }

  def get(
    internalId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, DataRetrievalError, Option[Registration]] =
    EitherT {
      eclRegistrationConnector
        .getRegistration(internalId)
        .map(registration => Right(Some(registration)))
        .recover {
          case _: NotFoundException                         =>
            Right(None)
          case _ @UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
            Right(None)
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr)                                => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def upsertRegistration(
    registration: Registration
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      eclRegistrationConnector
        .upsertRegistration(registration)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def deleteRegistration(
    internalId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      eclRegistrationConnector
        .deleteRegistration(internalId)
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

  def submitRegistration(
    internalId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, DataRetrievalError, CreateEclSubscriptionResponse] =
    EitherT {
      eclRegistrationConnector
        .submitRegistration(internalId)
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

  def getRegistrationValidationErrors(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, Option[DataValidationError]] =
    EitherT {
      eclRegistrationConnector
        .getRegistrationValidationErrors(internalId)
        .value
        .map(str => Right(str.map(x => DataValidationError(x))))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def registerEntityType(
    entityType: EntityType,
    mode: Mode
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, String] =
    entityType match {
      case UkLimitedCompany | UnlimitedCompany | RegisteredSociety =>
        EitherT {
          incorporatedEntityIdentificationFrontendConnector
            .createIncorporatedEntityJourney(entityType, mode)
            .map(response => Right(response.journeyStartUrl))
            .recover {
              case error @ UpstreamErrorResponse(message, code, _, _)
                  if UpstreamErrorResponse.Upstream5xxResponse
                    .unapply(error)
                    .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        }

      case SoleTrader =>
        EitherT {
          soleTraderIdentificationFrontendConnector
            .createSoleTraderJourney(mode)
            .map(response => Right(response.journeyStartUrl))
            .recover {
              case error @ UpstreamErrorResponse(message, code, _, _)
                  if UpstreamErrorResponse.Upstream5xxResponse
                    .unapply(error)
                    .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        }

      case GeneralPartnership | ScottishPartnership | LimitedPartnership | ScottishLimitedPartnership |
          LimitedLiabilityPartnership =>
        EitherT {
          partnershipIdentificationFrontendConnector
            .createPartnershipJourney(entityType, mode)
            .map(response => Right(response.journeyStartUrl))
            .recover {
              case error @ UpstreamErrorResponse(message, code, _, _)
                  if UpstreamErrorResponse.Upstream5xxResponse
                    .unapply(error)
                    .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        }

      case entityType: EntityType =>
        EitherT.leftT(DataRetrievalError.InternalUnexpectedError(s"Invalid entityType: $entityType", None))
    }

  def getSubscriptionStatus(
    businessPartnerId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, EclSubscriptionStatus] =
    EitherT {
      eclRegistrationConnector
        .getSubscriptionStatus(businessPartnerId)
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

  def getSubscription(
    eclRegistration: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, GetSubscriptionResponse] =
    EitherT {
      eclRegistrationConnector
        .getSubscription(eclRegistration)
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

  def transformToRegistration(
    registration: Registration,
    getSubscriptionResponse: GetSubscriptionResponse
  ): Registration = {
    val primaryContact      = getSubscriptionResponse.primaryContactDetails
    val secondaryContact    = getSubscriptionResponse.secondaryContactDetails
    val subscriptionAddress = getSubscriptionResponse.correspondenceAddressDetails

    val firstContactDetails: ContactDetails = ContactDetails(
      Some(primaryContact.name),
      Some(primaryContact.positionInCompany),
      Some(primaryContact.emailAddress),
      Some(primaryContact.telephone)
    )
    val secondContactDetails                = secondaryContact match {
      case Some(value) =>
        ContactDetails(
          Some(value.name),
          Some(value.positionInCompany),
          Some(value.emailAddress),
          Some(value.telephone)
        )
      case _           => ContactDetails.empty
    }
    val secondContactPresent                = Some(secondaryContact.isDefined)

    val contacts: Contacts = Contacts(firstContactDetails, secondContactPresent, secondContactDetails)

    val businessSector: BusinessSector =
      BusinessSector.transformFromSubscriptionResponse(getSubscriptionResponse.additionalDetails.businessSector)
    val address: EclAddress            = EclAddress(
      organisation = None,
      addressLine1 = Some(subscriptionAddress.addressLine1),
      addressLine2 = subscriptionAddress.addressLine2,
      addressLine3 = subscriptionAddress.addressLine3,
      addressLine4 = subscriptionAddress.addressLine4,
      region = None,
      postCode = subscriptionAddress.postCode,
      poBox = None,
      countryCode = subscriptionAddress.countryCode
    )
    registration.copy(
      contacts = contacts,
      businessSector = Some(businessSector),
      contactAddress = Some(address),
      amlSupervisor = Some(getAmlSupervisor(getSubscriptionResponse.additionalDetails.amlSupervisor))
    )
  }

  private def getAmlSupervisor(amlSupervisor: String): AmlSupervisor = {
    val sanitisedAmlSupervisor                         = amlSupervisor.filterNot(_.isWhitespace).toLowerCase
    val amlProfessionalBodySupervisors: Option[String] =
      appConfig.amlProfessionalBodySupervisors.find(p => p.toLowerCase() == sanitisedAmlSupervisor)

    if (amlProfessionalBodySupervisors.isEmpty) {
      sanitisedAmlSupervisor match {
        case "hmrc" => AmlSupervisor(AmlSupervisorType.Hmrc, None)
        case e      => throw new IllegalStateException(s"AML supervisor returned in GetSubscriptionResponse not valid: $e")
      }
    } else {
      AmlSupervisor(AmlSupervisorType.Other, Some(amlSupervisor))
    }
  }
}
