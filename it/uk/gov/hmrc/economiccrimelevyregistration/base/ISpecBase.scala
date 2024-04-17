/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Status => _, _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test._
import play.api.{Application, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.arbEntityType
import uk.gov.hmrc.economiccrimelevyregistration.generators.Generators
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.isOther
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

abstract class ISpecBase
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with DefaultAwaitTimeout
    with Writeables
    with RouteInvokers
    with Results
    with Status
    with HeaderNames
    with MimeTypes
    with ResultExtractors
    with OptionValues
    with WireMockHelper
    with WireMockStubs
    with Generators {

  implicit val arbString: Arbitrary[String]    = Arbitrary(Gen.alphaNumStr.retryUntil(_.nonEmpty))
  implicit lazy val system: ActorSystem        = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit def ec: ExecutionContext            = global

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"                         -> false,
    "auditing.enabled"                        -> false,
    "features.grsStubEnabled"                 -> false,
    "features.alfStubEnabled"                 -> false,
    "features.enrolmentStoreProxyStubEnabled" -> false,
    "features.getSubscriptionEnabled"         -> false
  ) ++ setWireMockPort(
    "auth",
    "economic-crime-levy-registration",
    "economic-crime-levy-calculator",
    "incorporated-entity-identification-frontend",
    "sole-trader-identification-frontend",
    "partnership-identification-frontend",
    "address-lookup-frontend",
    "enrolment-store-proxy",
    "email"
  )

  val contextPath: String = "register-for-economic-crime-levy"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .in(Mode.Test)
      .build()

  val appConfig: AppConfig              = app.injector.instanceOf[AppConfig]
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val testCurrentDate: LocalDate = LocalDate.now()
  val testEclTaxYear: EclTaxYear = EclTaxYear.fromCurrentDate(testCurrentDate)

  /*
  This is to initialise the app before running any tests, as it is lazy by default in org.scalatestplus.play.BaseOneAppPerSuite.
  It enables us to include behaviour tests that call routes within the `should` part of a test but before `in`.
   */
  locally { val _ = app }

  override def beforeAll(): Unit = {
    startWireMock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    resetWireMock()
    super.afterEach()
  }

  def callRoute[A](fakeRequest: FakeRequest[A], requiresAuth: Boolean = true)(implicit
    app: Application,
    w: Writeable[A]
  ): Future[Result] = {
    val errorHandler = app.errorHandler

    val req = if (requiresAuth) fakeRequest.withSession("authToken" -> "test") else fakeRequest

    route(app, req) match {
      case None         => fail("Route does not exist")
      case Some(result) =>
        result.recoverWith { case t: Throwable =>
          errorHandler.onServerError(req, t)
        }
    }
  }

  def html(result: Future[Result]): String = {
    contentType(result) shouldBe Some("text/html")
    Jsoup.parse(contentAsString(result)).html()
  }

  val apRevenueMin: Double          = 10_200_000
  val apRevenueMax: Double          = 999_999_999.99
  val apLengthMin                   = 0
  val apLengthMax                   = 364
  def randomApRevenue(): BigDecimal = bigDecimalInRange(apRevenueMin.doubleValue, apRevenueMax.doubleValue).sample.value

  def generateOtherEntityType: Gen[EntityType] =
    Arbitrary.arbitrary[EntityType].retryUntil(entityType => isOther(entityType))
}
