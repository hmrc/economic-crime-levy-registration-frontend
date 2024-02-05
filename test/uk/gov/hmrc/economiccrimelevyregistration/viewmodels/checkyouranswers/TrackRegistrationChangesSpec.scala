package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkyouranswers

import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers.TrackRegistrationChanges

class TrackRegistrationChangesSpec {

  def defaultEclRegistration(registration: Registration): Registration =
    registration.copy(registrationType = Some(Amendment))
}

final case class TestTrackEclReturnChanges(
  registration: Registration,
  getSubscriptionResponse: Option[GetSubscriptionResponse]
) extends TrackRegistrationChanges
