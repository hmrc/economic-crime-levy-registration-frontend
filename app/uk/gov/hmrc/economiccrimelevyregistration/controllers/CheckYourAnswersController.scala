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

import cats.implicits.toTraverseOps
import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.services._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyregistration.views.html._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import java.util.Base64
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  validateRegistrationData: ValidatedRegistrationAction,
  emailService: EmailService,
  otherRegistrationPdfView: OtherRegistrationPdfView,
  amendRegistrationPdfView: AmendRegistrationPdfView,
  appConfig: AppConfig,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getRegistrationData andThen validateRegistrationData).async { implicit request =>
      if (appConfig.getSubscriptionEnabled && request.registration.registrationType.contains(Amendment)) {
        registrationService.getSubscription(request.eclRegistrationReference.get).map { getSubscriptionResponse =>
          Ok(
            view(
              checkAnswers.CheckYourAnswersViewModel(
                request.registration,
                Some(getSubscriptionResponse),
                request.eclRegistrationReference
              )
            )
          )
        }
      } else {
        Future.successful(
          Ok(
            view(
              checkAnswers.CheckYourAnswersViewModel(
                request.registration,
                None,
                request.eclRegistrationReference
              )
            )
          )
        )
      }
    }

  private def getBase64EncodedPdf(
    checkYourAnswersViewModel: CheckYourAnswersViewModel,
    amendRegistrationPdfViewModel: AmendRegistrationPdfViewModel
  )(implicit
    request: RegistrationDataRequest[_]
  ): String = {
    val registrationType = checkYourAnswersViewModel.registrationType
    (checkYourAnswersViewModel.registration.entityType, registrationType) match {
      case (Some(_), Some(Amendment))                    =>
        createAndEncodeHtmlForPdf(checkYourAnswersViewModel, amendRegistrationPdfViewModel)
      case (Some(value), _) if EntityType.isOther(value) =>
        createAndEncodeHtmlForPdf(checkYourAnswersViewModel, amendRegistrationPdfViewModel)
      case (None, Some(Amendment))                       =>
        createAndEncodeHtmlForPdf(checkYourAnswersViewModel, amendRegistrationPdfViewModel)
      case _                                             =>
        ""
    }
  }

  def onSubmit(): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val getSubscriptionResponse =
      (if (appConfig.getSubscriptionEnabled) {
         Some(
           registrationService
             .getSubscription(request.eclRegistrationReference.get)
         )
       } else {
         None
       }).traverse(identity)

    val htmlView: Future[HtmlFormat.Appendable] = getSubscriptionResponse.map(response =>
      view(
        checkAnswers.CheckYourAnswersViewModel(
          request.registration,
          response,
          request.eclRegistrationReference
        )
      )
    )

    for {
      html                         <- htmlView
      base64EncodedHtmlView         = base64EncodeHtmlView(html.body)
      registration                  = request.registration
      getSubscriptionResponse      <- getSubscriptionResponse
      checkYourAnswersModel         =
        CheckYourAnswersViewModel(
          registration,
          getSubscriptionResponse,
          request.eclRegistrationReference
        )
      amendRegistrationPdfViewModel = AmendRegistrationPdfViewModel(registration, getSubscriptionResponse)
      base64EncodedHtmlViewForPdf   = getBase64EncodedPdf(checkYourAnswersModel, amendRegistrationPdfViewModel)
      _                            <- registrationService.upsertRegistration(registration =
                                        registration.copy(
                                          base64EncodedFields = Some(
                                            Base64EncodedFields(
                                              nrsSubmissionHtml = Some(base64EncodedHtmlView),
                                              dmsSubmissionHtml = (registration.entityType, registration.registrationType) match {
                                                case (Some(_), Some(Amendment))                    =>
                                                  Some(base64EncodedHtmlViewForPdf)
                                                case (Some(value), _) if EntityType.isOther(value) =>
                                                  Some(base64EncodedHtmlViewForPdf)
                                                case (None, Some(Amendment))                       =>
                                                  Some(base64EncodedHtmlViewForPdf)
                                                case _                                             =>
                                                  None
                                              }
                                            )
                                          )
                                        )
                                      )
      response                     <- registrationService.submitRegistration(request.internalId)
      _                             = request.registration.registrationType match {
                                        case Some(registrationType) =>
                                          registrationType match {
                                            case Initial   =>
                                              emailService.sendRegistrationSubmittedEmails(
                                                registration.contacts,
                                                response.eclReference,
                                                registration.entityType,
                                                request.additionalInfo,
                                                request.registration.carriedOutAmlRegulatedActivityInCurrentFy
                                              )
                                            case Amendment => emailService.sendAmendRegistrationSubmitted(registration.contacts)
                                          }
                                        case None                   => throw new IllegalStateException("Invalid contact details")
                                      }
      _                            <- registrationService.deleteRegistration(request.internalId)
      _                            <- registrationAdditionalInfoService.delete(request.internalId)
    } yield {
      val session = registration.entityType match {
        case Some(value) if EntityType.isOther(value) => request.session
        case _                                        =>
          request.session ++ Seq(
            SessionKeys.EclReference -> response.eclReference
          )
      }

      val updatedSession = session ++ Seq(
        SessionKeys.FirstContactEmailAddress -> registration.contacts.firstContactDetails.emailAddress
          .getOrElse(throw new IllegalStateException("First contact email address not found in registration data"))
      )

      Redirect((registration.entityType, registration.registrationType) match {
        case (Some(value), Some(Initial)) if EntityType.isOther(value) =>
          routes.RegistrationReceivedController.onPageLoad()
        case (Some(_), Some(Amendment))                                =>
          routes.AmendmentRequestedController.onPageLoad()
        case (None, Some(Amendment))                                   =>
          routes.AmendmentRequestedController.onPageLoad()
        case _                                                         =>
          routes.RegistrationSubmittedController.onPageLoad()
      }).withSession(
        registration.contacts.secondContactDetails.emailAddress.fold(updatedSession)(email =>
          updatedSession ++ Seq(SessionKeys.SecondContactEmailAddress -> email)
        )
      )
    }
  }

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

  def createAndEncodeHtmlForPdf(
    checkYourAnswersViewModel: CheckYourAnswersViewModel,
    amendRegistrationPdfViewModel: AmendRegistrationPdfViewModel
  )(implicit request: RegistrationDataRequest[_]): String = {
    val date         = LocalDate.now
    val organisation = checkYourAnswersViewModel.organisationDetails(LiabilityYearSummary.row())
    val contact      = checkYourAnswersViewModel.contactDetails()
    val otherEntity  = checkYourAnswersViewModel.otherEntityDetails()

    checkYourAnswersViewModel.registrationType match {
      case Some(Amendment) =>
        base64EncodeHtmlView(
          amendRegistrationPdfView(
            ViewUtils.formatLocalDate(date),
            amendRegistrationPdfViewModel
          ).toString()
        )
      case _               =>
        base64EncodeHtmlView(
          otherRegistrationPdfView(
            ViewUtils.formatLocalDate(date),
            organisation.copy(rows = organisation.rows.map(_.copy(actions = None))),
            contact.copy(rows = contact.rows.map(_.copy(actions = None))),
            otherEntity.copy(
              rows = otherEntity.rows.map(_.copy(actions = None))
            )
          ).toString()
        )
    }
  }
}
