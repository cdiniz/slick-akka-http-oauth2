package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.OAuthAuthorizationCode
import persistence.entities.SlickTables.OauthAuthorizationCodeTable
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait OAuthAuthorizationCodesDal extends BaseDalImpl[OauthAuthorizationCodeTable,OAuthAuthorizationCode]{
  def findByCode(code: String): Future[Option[OAuthAuthorizationCode]]
  def delete(code: String): Future[Int]
}

class OAuthAuthorizationCodesDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends OAuthAuthorizationCodesDal {
  override def findByCode(code: String): Future[Option[OAuthAuthorizationCode]] = {
    val expireAt = new Timestamp(new DateTime().minusMinutes(30).getMillis)
    findByFilter(authCode => authCode.code === code && authCode.createdAt > expireAt).map(_.headOption)
  }

  override def delete(code: String): Future[Int] = deleteByFilter(_.code === code)

}