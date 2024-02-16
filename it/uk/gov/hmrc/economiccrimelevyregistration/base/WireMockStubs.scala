/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.base.deregister.DeregistrationStubs

trait WireMockStubs
    extends EclTestData
    with AuthStubs
    with GrsStubs
    with AlfStubs
    with EclRegistrationStubs
    with RegistrationAdditionalInfoStubs
    with EclCalculatorStubs
    with EnrolmentStoreProxyStubs
    with EmailStubs
    with SessionDataStubs
    with DeregistrationStubs {

  def stubAuthorisedWithNoGroupEnrolment(): StubMapping = {
    stubAuthorised()
    stubNoGroupEnrolment()
  }
}
