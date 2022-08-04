/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EconomicCrimeLevyRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{IdentifierRequest, OptionalDataRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(eclRegistrationConnector: EconomicCrimeLevyRegistrationConnector)
      extends DataRetrievalActionImpl(eclRegistrationConnector) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to 'None' in the request" in {

        val eclRegistrationConnector = mock[EconomicCrimeLevyRegistrationConnector]
        when(eclRegistrationConnector.getRegistration("id")) thenReturn Future(None)
        val action                   = new Harness(eclRegistrationConnector)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), "id")).futureValue

        result.userAnswers must not be defined
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {

        val eclRegistrationConnector = mock[EconomicCrimeLevyRegistrationConnector]
        when(eclRegistrationConnector.getRegistration("id")) thenReturn Future(Some(Registration("id")))
        val action                   = new Harness(eclRegistrationConnector)

        val result = action.callTransform(new IdentifierRequest(FakeRequest(), "id")).futureValue

        result.userAnswers mustBe defined
      }
    }
  }
}
