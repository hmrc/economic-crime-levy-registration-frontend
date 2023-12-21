package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyregistration.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.AddressLookupContinueError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AddressLookupContinueService @Inject() (addressLookupFrontendConnector: AddressLookupFrontendConnector)(implicit
  hc: HeaderCarrier,
  ec: ExecutionContext
) {

  def initJourney(ukMode: Boolean, mode: Mode): EitherT[Future, AddressLookupContinueError, String] =
    EitherT {
      addressLookupFrontendConnector
        .initJourney(ukMode, mode)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(AddressLookupContinueError.BadGateway(message, code))
          case NonFatal(thr) => Left(AddressLookupContinueError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def getAddress(
    addressId: String
  ): EitherT[Future, AddressLookupContinueError, AlfAddressData] =
    EitherT {
      addressLookupFrontendConnector
        .getAddress(addressId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(AddressLookupContinueError.BadGateway(message, code))
          case NonFatal(thr) => Left(AddressLookupContinueError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }
}
