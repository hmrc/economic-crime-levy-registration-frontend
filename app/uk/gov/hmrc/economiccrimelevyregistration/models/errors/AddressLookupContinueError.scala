package uk.gov.hmrc.economiccrimelevyregistration.models.errors

trait AddressLookupContinueError

object AddressLookupContinueError {

  case class BadGateway(reason: String, code: Int) extends AddressLookupContinueError

  case class InternalUnexpectedError(message: String, cause: Option[Throwable]) extends AddressLookupContinueError
}
