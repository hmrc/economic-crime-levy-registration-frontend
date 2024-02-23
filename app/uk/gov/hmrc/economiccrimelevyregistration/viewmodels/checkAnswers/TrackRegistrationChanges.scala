/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers

import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration, RegistrationAdditionalInfo}

import java.time.LocalDate

trait TrackRegistrationChanges {

  val registration: Registration

  val additionalInfo: Option[RegistrationAdditionalInfo]

  val getSubscriptionResponse: Option[GetSubscriptionResponse]

  val isAmendRegistration: Boolean   = registration.registrationType.contains(Amendment)
  val isInitialRegistration: Boolean = registration.registrationType.contains(Initial)

  def getFullName(firstName: String, lastName: String): String = s"$firstName $lastName"

  val hasBusinessSectorChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      registration.businessSector match {
        case Some(businessSector) =>
          !businessSector.toString
            .replace(" ", "")
            .equalsIgnoreCase(response.additionalDetails.businessSector.replace(" ", ""))
        case None                 => false
      }
    case None           => false
  }

  val hasAddressLine1Changed: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      registration.contactAddress match {
        case Some(registrationContact) =>
          !registrationContact.addressLine1.contains(response.correspondenceAddressDetails.addressLine1)
        case None                      => false
      }
    case None           => false
  }

  val hasAddressLine2Changed: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      response.correspondenceAddressDetails.addressLine2 match {
        case Some(addressLine2) =>
          registration.contactAddress match {
            case Some(contactAddress) => !contactAddress.addressLine2.contains(addressLine2)
            case None                 => true

          }
        case None               =>
          registration.contactAddress match {
            case Some(_) => true
            case None    => false

          }
      }
    case None           => false
  }

  val hasAddressLine3Changed: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      response.correspondenceAddressDetails.addressLine3 match {
        case Some(addressLine3) =>
          registration.contactAddress match {
            case Some(contactAddress) => !contactAddress.addressLine3.contains(addressLine3)
            case None                 => true

          }
        case None               =>
          registration.contactAddress match {
            case Some(_) => true
            case None    => false

          }
      }
    case None           => false
  }

  val hasAddressLine4Changed: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      response.correspondenceAddressDetails.addressLine4 match {
        case Some(addressLine4) =>
          registration.contactAddress match {
            case Some(contactAddress) => !contactAddress.addressLine4.contains(addressLine4)
            case None                 => true

          }
        case None               =>
          registration.contactAddress match {
            case Some(_) => true
            case None    => false

          }
      }
    case None           => false
  }

  val hasAddressChanged: Boolean =
    hasAddressLine1Changed && hasAddressLine2Changed && hasAddressLine3Changed && hasAddressLine4Changed

  val hasAmlSupervisorChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      registration.amlSupervisor match {
        case Some(amlSupervisor) =>
          !(amlSupervisor.supervisorType.toString.equalsIgnoreCase(response.additionalDetails.amlSupervisor) ||
            amlSupervisor.otherProfessionalBody.contains(response.additionalDetails.amlSupervisor))
        case None                => false
      }
    case None           => false
  }

  val hasFirstContactNameChanged: Boolean  = getSubscriptionResponse match {
    case Some(response) => !registration.contacts.firstContactDetails.name.contains(response.primaryContactDetails.name)
    case None           => false
  }
  val hasFirstContactRoleChanged: Boolean  = getSubscriptionResponse match {
    case Some(response) =>
      !registration.contacts.firstContactDetails.role.contains(response.primaryContactDetails.positionInCompany)
    case None           => false
  }
  val hasFirstContactEmailChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      !registration.contacts.firstContactDetails.emailAddress.contains(response.primaryContactDetails.emailAddress)
    case None           => false
  }
  val hasFirstContactPhoneChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      !registration.contacts.firstContactDetails.telephoneNumber.contains(response.primaryContactDetails.telephone)
    case None           => false
  }

  val hasSecondContactDetailsPresentChanged: Boolean = getSubscriptionResponse match {
    case Some(response) => !registration.contacts.secondContact.contains(response.secondaryContactDetails.isDefined)
    case None           => false
  }

  val hasSecondContactNameChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      response.secondaryContactDetails match {
        case Some(secondaryContactDetails) =>
          !registration.contacts.secondContactDetails.name.contains(secondaryContactDetails.name)
        case None                          =>
          registration.contacts.secondContactDetails.name match {
            case Some(_) => true
            case None    => false
          }
      }
    case None           => false
  }

  val hasSecondContactRoleChanged: Boolean  = getSubscriptionResponse match {
    case Some(response) =>
      response.secondaryContactDetails match {
        case Some(secondaryContactDetails) =>
          !registration.contacts.secondContactDetails.role.contains(secondaryContactDetails.positionInCompany)
        case None                          =>
          registration.contacts.secondContactDetails.role match {
            case Some(_) => true
            case None    => false
          }
      }
    case None           => false
  }
  val hasSecondContactPhoneChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      response.secondaryContactDetails match {
        case Some(secondaryContactDetails) =>
          !registration.contacts.secondContactDetails.telephoneNumber.contains(secondaryContactDetails.telephone)
        case None                          =>
          registration.contacts.secondContactDetails.telephoneNumber match {
            case Some(_) => true
            case None    => false
          }
      }
    case None           => false
  }
  val hasSecondContactEmailChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      response.secondaryContactDetails match {
        case Some(secondaryContactDetails) =>
          !registration.contacts.secondContactDetails.emailAddress.contains(secondaryContactDetails.emailAddress)
        case None                          =>
          registration.contacts.secondContactDetails.emailAddress match {
            case Some(_) => true
            case None    => false
          }
      }
    case None           => false
  }

  private val hasLiabilityStartDateChanged: Boolean = getSubscriptionResponse match {
    case Some(response) =>
      additionalInfo.exists(
        _.liabilityStartDate.exists(dateStr =>
          dateStr != LocalDate.parse(response.additionalDetails.liabilityStartDate)
        )
      )
    case None           => false
  }

  val hasAnyAmends: Boolean = Seq(
    hasBusinessSectorChanged,
    hasAddressChanged,
    hasAmlSupervisorChanged,
    hasFirstContactNameChanged,
    hasFirstContactRoleChanged,
    hasFirstContactEmailChanged,
    hasFirstContactPhoneChanged,
    hasSecondContactDetailsPresentChanged,
    hasSecondContactNameChanged,
    hasSecondContactRoleChanged,
    hasSecondContactPhoneChanged,
    hasSecondContactEmailChanged,
    hasLiabilityStartDateChanged
  ).contains(true)
}
