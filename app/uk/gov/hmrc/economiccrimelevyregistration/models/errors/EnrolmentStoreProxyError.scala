package uk.gov.hmrc.economiccrimelevyregistration.models.errors

trait EnrolmentStoreProxyError

object EnrolmentStoreProxyError {
  case class InternalUnexpectedError(message: String, cause: Option[Throwable]) extends EnrolmentStoreProxyError
  case class BadGateway(reason: String, code: Int) extends EnrolmentStoreProxyError

}
