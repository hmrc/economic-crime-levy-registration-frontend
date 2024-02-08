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

import cats.data.EitherT
import cats.implicits.toTraverseOps
import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EmailService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendRegistrationPdfView, CheckYourAnswersView, ErrorTemplate, OtherRegistrationPdfView}
import uk.gov.hmrc.http.HeaderCarrier
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
  registrationService: EclRegistrationService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  emailService: EmailService,
  otherRegistrationPdfView: OtherRegistrationPdfView,
  amendRegistrationPdfView: AmendRegistrationPdfView,
  sessionService: SessionService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      registrationService
        .getRegistrationValidationErrors(request.internalId)
        .asResponseError
        .foldF(
          error => Future.successful(routeError(error)),
          {
            case Some(error) =>
              Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
            case None        =>
              if (appConfig.getSubscriptionEnabled && request.registration.registrationType.contains(Amendment)) {
                routeWithSubscription
              } else { routeWithoutSubscription }
          }
        )
    }

  private def getBase64EncodedPdf(
    checkYourAnswersViewModel: CheckYourAnswersViewModel,
    amendRegistrationPdfViewModel: AmendRegistrationPdfViewModel
  )(implicit
    request: RegistrationDataRequest[_]
  ) = {
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

  private def routeWithSubscription(implicit request: RegistrationDataRequest[_]) =
    registrationService
      .getSubscription(request.eclRegistrationReference.get)
      .map { getSubscriptionResponse =>
        Ok(
          view(
            CheckYourAnswersViewModel(
              request.registration,
              Some(getSubscriptionResponse),
              request.eclRegistrationReference
            )
          )
        )
      }
      .asResponseError
      .fold(
        error => routeError(error),
        success => success
      )

  private def routeWithoutSubscription(implicit request: RegistrationDataRequest[_]) =
    Future.successful(
      Ok(
        view(
          CheckYourAnswersViewModel(
            request.registration,
            None,
            request.eclRegistrationReference
          )
        )
      )
    )
  def onSubmit(): Action[AnyContent]                                                 = (authorise andThen getRegistrationData).async { implicit request =>
    val registration = request.registration
    (for {
      getSubscriptionResponse      <- fetchSubscription.asResponseError
      htmlView                      = createHtmlView(getSubscriptionResponse)
      base64EncodedHtmlView         = base64EncodeHtmlView(htmlView.body)
      checkYourAnswersModel         =
        CheckYourAnswersViewModel(registration, getSubscriptionResponse, request.eclRegistrationReference)
      amendRegistrationPdfViewModel =
        AmendRegistrationPdfViewModel(registration, getSubscriptionResponse, request.eclRegistrationReference)
      base64EncodedHtmlViewForPdf   = getBase64EncodedPdf(checkYourAnswersModel, amendRegistrationPdfViewModel)
      _                            <- registrationService
                                        .upsertRegistration(
                                          getRegistrationWithEncodedFields(registration, base64EncodedHtmlView, base64EncodedHtmlViewForPdf)
                                        )
                                        .asResponseError
      response                     <- registrationService.submitRegistration(request.internalId).asResponseError
      _                            <- sendEmail(registration, request.additionalInfo, response.eclReference).asResponseError
      email                         = registration.contacts.firstContactDetails.emailAddress
    } yield (response, email)).fold(
      error => routeError(error),
      data => {
        val session = registration.entityType match {
          case Some(value) if EntityType.isOther(value) => request.session
          case _                                        =>
            request.session ++ Seq(
              SessionKeys.EclReference -> data._1.eclReference
            )
        }

        val updatedSession = session ++ Seq(
          SessionKeys.FirstContactEmailAddress -> registration.contacts.firstContactDetails.emailAddress
            .getOrElse(throw new IllegalStateException("First contact email address not found in registration data")),
          SessionKeys.ContactAddress           -> Json
            .toJson(
              registration.contactAddress
                .getOrElse(throw new IllegalStateException("Contact address not found in registration data"))
            )
            .toString
        ) ++ registration.contacts.secondContactDetails.emailAddress.fold(Seq.empty[(String, String)])(email =>
          Seq(SessionKeys.SecondContactEmailAddress -> email)
        )

        sessionService.upsert(SessionData(request.internalId, updatedSession.data))

        Redirect(getNextPage(registration)).withSession(updatedSession)
      }
    )
  }

  private def createHtmlView(
    getSubscriptionResponse: Option[GetSubscriptionResponse]
  )(implicit request: RegistrationDataRequest[_]) =
    view(
      CheckYourAnswersViewModel(
        request.registration,
        getSubscriptionResponse,
        request.eclRegistrationReference
      )
    )

  private def fetchSubscription(implicit request: RegistrationDataRequest[_]) = {
    val getSubscriptionResponse =
      (if (appConfig.getSubscriptionEnabled) {
         Some(
           registrationService
             .getSubscription(request.eclRegistrationReference.get)
         )
       } else {
         None
       }).traverse(identity)
    getSubscriptionResponse
  }

  private def getNextPage(registration: Registration) =
    (registration.entityType, registration.registrationType) match {
      case (Some(value), Some(Initial)) if EntityType.isOther(value) =>
        routes.RegistrationReceivedController.onPageLoad()
      case (Some(_), Some(Amendment))                                =>
        routes.AmendmentRequestedController.onPageLoad()
      case (None, Some(Amendment))                                   =>
        routes.AmendmentRequestedController.onPageLoad()
      case _                                                         =>
        routes.RegistrationSubmittedController.onPageLoad()
    }

  private def sendEmail(
    registration: Registration,
    additionalInfo: Option[RegistrationAdditionalInfo],
    eclReference: String
  )(implicit hc: HeaderCarrier, messages: Messages): EitherT[Future, DataRetrievalError, Unit] =
    registration.registrationType match {
      case Some(Initial)   =>
        emailService.sendRegistrationSubmittedEmails(
          registration.contacts,
          eclReference,
          registration.entityType,
          additionalInfo,
          registration.carriedOutAmlRegulatedActivityInCurrentFy
        )
      case Some(Amendment) =>
        emailService.sendAmendRegistrationSubmitted(registration.contacts)
      case None            =>
        EitherT[Future, DataRetrievalError, Unit](
          Future.successful(Left(DataRetrievalError.InternalUnexpectedError("Registration type is null.", None)))
        )
    }

  private def getRegistrationWithEncodedFields(
    registration: Registration,
    base64EncodedHtmlView: String,
    base64EncodedHtmlViewForPdf: String
  ) =
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
