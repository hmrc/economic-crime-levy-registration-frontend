package uk.gov.hmrc.economiccrimelevyregistration.models.errors

trait SessionError

object SessionError {
  case class BadGateway(reason: String, code: Int) extends SessionError

  case class InternalUnexpectedError(message: String, cause: Option[Throwable]) extends SessionError
}
