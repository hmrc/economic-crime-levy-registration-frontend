package uk.gov.hmrc.economiccrimelevyregistration.services

import uk.gov.hmrc.economiccrimelevyregistration.connectors.SessionRetrievalConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.{RegistrationAdditionalInfo, SessionData}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class SessionRetrievalService @Inject() (sessionRetrievalConnector: SessionRetrievalConnector) {

  def get(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Option[SessionData]] =
    sessionRetrievalConnector.get(internalId)

  def createOrUpdate(
    internalId: String,
    eclReference: Option[String]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    sessionRetrievalConnector.upsert(RegistrationAdditionalInfo(internalId, None, eclReference))

  def delete(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    sessionRetrievalConnector.delete(internalId)

}
