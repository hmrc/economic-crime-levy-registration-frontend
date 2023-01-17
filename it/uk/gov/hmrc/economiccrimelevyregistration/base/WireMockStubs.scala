/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData

trait WireMockStubs
    extends EclTestData
    with AuthStubs
    with GrsStubs
    with AlfStubs
    with EclRegistrationStubs
    with EclReturnStubs
    with EnrolmentStoreProxyStubs {

  def stubAuthorisedWithNoGroupEnrolment(): StubMapping = {
    stubAuthorised()
    stubNoGroupEnrolment()
  }
}
