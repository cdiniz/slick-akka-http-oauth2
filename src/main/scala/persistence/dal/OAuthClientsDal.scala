package persistence.dal

import persistence.entities.{Account, OAuthClient}
import persistence.entities.SlickTables.OauthClientTable
import scala.concurrent.ExecutionContext.Implicits.global
import utils.{Configuration, PersistenceModule}
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import scala.concurrent.Future


trait OAuthClientsDal extends BaseDalImpl[OauthClientTable,OAuthClient]{
  def validate(clientId: String, clientSecret: String, grantType: String): Future[Boolean]
  def findByClientId(clientId: String): Future[Option[OAuthClient]]
  def findClientCredentials(clientId: String, clientSecret: String): Future[Option[Account]]
}

class OAuthClientsDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends OAuthClientsDal {
  override def validate(clientId: String, clientSecret: String, grantType: String): Future[Boolean] = {
    findByFilter(oauthClient => oauthClient.clientId === clientId && oauthClient.clientSecret === clientSecret)
      .map(_.headOption.map(client => grantType == client.grantType || grantType == "refresh_token")
        .getOrElse(false))
  }
  override def findClientCredentials(clientId: String, clientSecret: String): Future[Option[Account]] = {
    for {
      accountId <- findByFilter(oauthClient => oauthClient.clientId === clientId && oauthClient.clientSecret === clientSecret).map(_.headOption.map(_.ownerId))
      account <- modules.accountsDal.findById(accountId.get)
    } yield account
  }
  override def findByClientId(clientId: String): Future[Option[OAuthClient]] = {
    findByFilter(_.clientId === clientId).map(_.headOption)
  }

}