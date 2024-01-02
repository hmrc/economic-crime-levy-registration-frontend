package uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts

import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

trait ContactsUtils {

  implicit class FirstAndLastNameHelper(request: RegistrationDataRequest[_]) {

    def firstContactName: Either[DataRetrievalError, String] =
      request.registration.contacts.firstContactDetails.name match {
        case Some(value) => Right(value)
        case None        => Left(DataRetrievalError.FieldNotFound("No first contact name found in registration data"))
      }

    def secondContactName: Either[DataRetrievalError, String] =
      request.registration.contacts.secondContactDetails.name match {
        case Some(value) => Right(value)
        case None        => Left(DataRetrievalError.FieldNotFound("No second contact name found in registration data"))
      }
  }
}
