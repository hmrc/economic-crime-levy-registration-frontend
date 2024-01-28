package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RegistrationNotLiableAuditEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class AuditService @Inject() (auditConnector: AuditConnector)(implicit
  ec: ExecutionContext
) {

  private def sendNotLiableAuditEvent(internalId: String, notLiableReason: NotLiableReason)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    EitherT {
      auditConnector
        .sendExtendedEvent(RegistrationNotLiableAuditEvent(internalId, notLiableReason).extendedDataEvent)

      Future.unit //TODO - what?!
    }
}
