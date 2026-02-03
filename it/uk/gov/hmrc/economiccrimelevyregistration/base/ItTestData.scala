package uk.gov.hmrc.economiccrimelevyregistration.base

import scala.util.Random
import java.util.UUID

object ItTestData {

  private val EmailLocalPartLength = 10
  private val PhoneMin             = 100000000
  private val PhoneRange           = 900000000

  def nonEmptyAlphaNum(len: Int = 8): String =
    Random.alphanumeric.take(len).mkString match {
      case "" => "X"
      case s  => s
    }

  def uuid(): String =
    UUID.randomUUID().toString

  def email(): String =
    s"${nonEmptyAlphaNum(EmailLocalPartLength).toLowerCase}@example.com"

  def phone(): String =
    s"07${Random.nextInt(PhoneRange) + PhoneMin}"
}
