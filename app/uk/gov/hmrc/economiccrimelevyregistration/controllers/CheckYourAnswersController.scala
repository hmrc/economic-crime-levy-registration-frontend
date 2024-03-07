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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.StoreUrlAction
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EmailService, RegistrationAdditionalInfoService}
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
  storeUrl: StoreUrlAction,
  registrationService: EclRegistrationService,
  val controllerComponents: MessagesControllerComponents,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  view: CheckYourAnswersView,
  emailService: EmailService,
  otherRegistrationPdfView: OtherRegistrationPdfView,
  amendRegistrationPdfView: AmendRegistrationPdfView,
  appConfig: AppConfig
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getRegistrationData andThen storeUrl).async { implicit request =>
      registrationService
        .getRegistrationValidationErrors(request.internalId)
        .asResponseError
        .foldF(
          error => Future.successful(routeError(error)),
          {
            case Some(_) =>
              Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
            case None    =>
              if (appConfig.getSubscriptionEnabled && request.registration.registrationType.contains(Amendment)) {
                routeWithSubscription
              } else { routeWithoutSubscription }
          }
        )
    }

  private def getBase64EncodedPdf(
    checkYourAnswersViewModel: CheckYourAnswersViewModel,
    pdfViewModel: PdfViewModel
  )(implicit
    request: RegistrationDataRequest[_]
  ) = {
    val registrationType = checkYourAnswersViewModel.registrationType
    (checkYourAnswersViewModel.registration.entityType, registrationType) match {
      case (_, Some(Amendment))                          =>
        encodeAmendmentHtmlForPdf(pdfViewModel)
      case (Some(value), _) if EntityType.isOther(value) =>
        encodeOtherRegistrationHtmlForPdf(checkYourAnswersViewModel)
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
              request.eclRegistrationReference,
              request.additionalInfo
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
            request.eclRegistrationReference,
            request.additionalInfo
          )
        )
      )
    )

  def onSubmit(): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val registration = request.registration
    (for {
      getSubscriptionResponse    <- fetchSubscription.asResponseError
      htmlView                    = createHtmlView(getSubscriptionResponse)
      base64EncodedHtmlView       = base64EncodeHtmlView(htmlView.body)
      checkYourAnswersModel       =
        CheckYourAnswersViewModel(
          registration,
          getSubscriptionResponse,
          request.eclRegistrationReference,
          request.additionalInfo
        )
      pdfViewModel                =
        PdfViewModel(registration, getSubscriptionResponse, request.eclRegistrationReference, request.additionalInfo)
      base64EncodedHtmlViewForPdf = getBase64EncodedPdf(checkYourAnswersModel, pdfViewModel)
      _                          <- registrationService
                                      .upsertRegistration(
                                        getRegistrationWithEncodedFields(registration, base64EncodedHtmlView, base64EncodedHtmlViewForPdf)
                                      )
                                      .asResponseError
      response                   <- registrationService.submitRegistration(request.internalId).asResponseError
      additionalInfo             <- valueOrError(request.additionalInfo, "additional info")
      updatedAdditionalInfo       = additionalInfo.copy(eclReference = Some(response.eclReference))
      _                          <- registrationAdditionalInfoService.upsert(updatedAdditionalInfo).asResponseError
      _                          <- sendEmail(registration, request.additionalInfo, response.eclReference).asResponseError
      firstEmail                 <- valueOrError(request.registration.contacts.firstContactDetails.emailAddress, "First contact email")
      address                    <- valueOrError(request.registration.contactAddress, "Contact address")
      secondEmail                 = request.registration.contacts.secondContactDetails.emailAddress
      amlRegulatedActivity       <-
        valueOrError(request.registration.carriedOutAmlRegulatedActivityInCurrentFy, "Aml regulated activity")
      liabilityYear              <- valueOrError(updatedAdditionalInfo.liabilityYear, "Liability Year")
    } yield (response, firstEmail, address, secondEmail, amlRegulatedActivity, liabilityYear)).fold(
      error => routeError(error),
      data => {
        val response      = data._1
        val firstEmail    = data._2
        val address       = data._3
        val secondEmail   = data._4
        val amlActivity   = data._5
        val liabilityYear = data._6

        val session = registration.entityType match {
          case Some(value) if EntityType.isOther(value) =>
            request.session ++ Seq(
                SessionKeys.FirstContactEmail    -> firstEmail,
                SessionKeys.AmlRegulatedActivity -> amlActivity.toString,
                SessionKeys.LiabilityYear        -> liabilityYear.asString,
              ) ++ secondEmail.fold(Seq.empty[(String, String)])(secondEmail => Seq(SessionKeys.SecondContactEmail -> secondEmail))
          case _                                        =>
            request.session ++ Seq(
              SessionKeys.EclReference      -> response.eclReference,
              SessionKeys.FirstContactEmail -> firstEmail,
              SessionKeys.ContactAddress    -> Json.stringify(Json.toJson(address))
            )
        }

        Redirect(getNextPage(registration)).withSession(session)
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
        request.eclRegistrationReference,
        request.additionalInfo
      )
    )

  private def fetchSubscription(implicit request: RegistrationDataRequest[_]) = {
    val getSubscriptionResponse =
      (if (appConfig.getSubscriptionEnabled && request.registration.registrationType.contains(Amendment)) {
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
  )(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): EitherT[Future, DataRetrievalError, Unit] =
    registration.registrationType match {
      case Some(Initial)                                       =>
        emailService.sendRegistrationSubmittedEmails(
          registration.contacts,
          eclReference,
          registration.entityType,
          additionalInfo,
          registration.carriedOutAmlRegulatedActivityInCurrentFy
        )(hc, messages)
      case Some(Amendment) if appConfig.getSubscriptionEnabled =>
        emailService.sendAmendRegistrationSubmitted(registration.contacts, registration.contactAddress)(hc, messages)
      case Some(Amendment)                                     => emailService.sendAmendRegistrationSubmitted(registration.contacts, None)(hc, messages)
      case None                                                =>
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

  def encodeAmendmentHtmlForPdf(
    pdfViewModel: PdfViewModel
  )(implicit request: RegistrationDataRequest[_]): String = {
    val date = LocalDate.now
    base64EncodeHtmlView(
      amendRegistrationPdfView(
        ViewUtils.formatLocalDate(date),
        pdfViewModel
      ).toString()
    )
  }

  def encodeOtherRegistrationHtmlForPdf(
    checkYourAnswersViewModel: CheckYourAnswersViewModel
  )(implicit request: RegistrationDataRequest[_]): String = {
    val date                      = LocalDate.now
    val organisation              = checkYourAnswersViewModel.organisationDetails()
    val contactDetails            = checkYourAnswersViewModel.contactDetails()
    val firstContact              = checkYourAnswersViewModel.firstContactDetails()
    val secondContact             = checkYourAnswersViewModel.secondContactDetails()
    val addressDetails            = checkYourAnswersViewModel.addressDetails()
    val otherEntity               = checkYourAnswersViewModel.otherEntityDetails()
    val hasSecondContact: Boolean = request.registration.contacts.secondContact.contains(true)

    base64EncodeHtmlView(
      otherRegistrationPdfView(
        ViewUtils.formatLocalDate(date),
        organisation.copy(rows = organisation.rows.map(_.copy(actions = None))),
        contactDetails.copy(rows = contactDetails.rows.map(_.copy(actions = None))),
        firstContact.copy(rows = firstContact.rows.map(_.copy(actions = None))),
        secondContact.copy(rows = secondContact.rows.map(_.copy(actions = None))),
        otherEntity.copy(rows = otherEntity.rows.map(_.copy(actions = None))),
        addressDetails.copy(rows = addressDetails.rows.map(_.copy(actions = None))),
        hasSecondContact
      ).toString()
    )
  }
}
