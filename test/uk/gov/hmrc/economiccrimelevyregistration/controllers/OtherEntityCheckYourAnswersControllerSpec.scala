/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, BodyParsers, Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.PublicBetaAction
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.navigation.OtherEntityCheckYourAnswersPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.views.html.OtherEntityCheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

import scala.concurrent.Future

class OtherEntityCheckYourAnswersControllerSpec extends SpecBase {

  val view: OtherEntityCheckYourAnswersView = app.injector.instanceOf[OtherEntityCheckYourAnswersView]
  val errorHandler: ErrorHandler            = app.injector.instanceOf[ErrorHandler]
  override val appConfig: AppConfig         = mock[AppConfig]
  val enabled: PublicBetaAction             = new PublicBetaAction(
    errorHandler = errorHandler,
    appConfig = appConfig,
    parser = app.injector.instanceOf[BodyParsers.Default]
  )

  val pageNavigator: OtherEntityCheckYourAnswersPageNavigator = new OtherEntityCheckYourAnswersPageNavigator(
  ) {
    override protected def navigateInNormalMode(
      registration: Registration
    ): Call = onwardRoute

    override protected def navigateInCheckMode(
      registration: Registration
    ): Call = onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new OtherEntityCheckYourAnswersController(
      messagesApi,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      pageNavigator,
      mcc,
      enabled,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { registration: Registration =>
      new TestContext(registration) {
        implicit val registrationDataRequest: RegistrationDataRequest[AnyContentAsEmpty.type] =
          RegistrationDataRequest(fakeRequest, registration.internalId, registration)
        implicit val messages: Messages                                                       = messagesApi.preferred(registrationDataRequest)

        when(appConfig.privateBetaEnabled).thenReturn(false)

        val result: Future[Result] = controller.onPageLoad()(registrationDataRequest)

        val otherEntityDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            OtherEntityTypeSummary.row(),
            BusinessNameSummary.row(),
            CharityRegistrationNumberSummary.row(),
            CompanyRegistrationNumberSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(otherEntityDetails)(
          fakeRequest,
          messages
        ).toString
      }
    }
  }

  "onSubmit" should {
    "save the selected entity type then redirect to the next page" in forAll {
      (registration: Registration, entityType: OtherEntityType, mode: Mode) =>
        new TestContext(registration) {
          when(appConfig.privateBetaEnabled).thenReturn(false)
          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", entityType.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }
  }
}
