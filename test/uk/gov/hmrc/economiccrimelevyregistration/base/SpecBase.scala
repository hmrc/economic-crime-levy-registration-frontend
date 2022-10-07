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

package uk.gov.hmrc.economiccrimelevyregistration.base

import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{FakeAuthorisedAction, FakeDataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.FakeNavigator
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyWordSpec
    with Matchers
    with TryValues
    with OptionValues
    with DefaultAwaitTimeout
    with FutureAwaits
    with Results
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaCheckPropertyChecks
    with EclTestData {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val ec: ExecutionContext     = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier        = HeaderCarrier()

  val internalId: String                                             = "test-id"
  val testRegistration: Registration                                 = Registration(internalId, None, None, None)
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type]               = FakeRequest()
  val appConfig: AppConfig                                           = app.injector.instanceOf[AppConfig]
  val messages: Messages                                             = messagesApi.preferred(fakeRequest)
  val bodyParsers: PlayBodyParsers                                   = app.injector.instanceOf[PlayBodyParsers]
  val fakeAuthorisedAction                                           = new FakeAuthorisedAction(bodyParsers)
  def fakeDataRetrievalAction(data: Registration = testRegistration) = new FakeDataRetrievalAction(data)

  def onwardRoute: Call = Call("GET", "/foo")

  val fakeNavigator = new FakeNavigator(onwardRoute)

  val mcc: DefaultMessagesControllerComponents = {
    val stub = stubControllerComponents()
    DefaultMessagesControllerComponents(
      new DefaultMessagesActionBuilderImpl(stubBodyParser(AnyContentAsEmpty), stub.messagesApi)(stub.executionContext),
      DefaultActionBuilder(stub.actionBuilder.parser)(stub.executionContext),
      stub.parsers,
      messagesApi,
      stub.langs,
      stub.fileMimeTypes,
      stub.executionContext
    )
  }

}
