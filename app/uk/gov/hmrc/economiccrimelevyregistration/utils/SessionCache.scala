package uk.gov.hmrc.economiccrimelevyregistration.utils

import play.api.mvc.Request
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionRetrievalService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SessionCache @Inject() (sessionRetrievalService: SessionRetrievalService)(implicit
  hc: HeaderCarrier,
  ec: ExecutionContext
) {

  def get(key: String, request: AuthorisedRequest[_]): Future[Option[String]] =
    Try {
      request.session(key)
    } match {
      case Success(value) => Future.successful(Some(value))
      case Failure(_)     =>
        sessionRetrievalService
          .get(request.internalId)
          .map(sessionDataOption => sessionDataOption.flatMap(sessionData => sessionData.values.get(key)))
    }

}
