package persistence.entities

import slick.driver.H2Driver.api._

object SlickTables {

  class AccountsTable(tag : Tag) extends BaseTable[Account](tag, "accounts") {
    def email = column[String]("email")
    def password = column[String]("password")
    def * = (id, email, password, createdAt) <> (Account.tupled, Account.unapply)
  }

  implicit val accountsTableQ : TableQuery[AccountsTable] = TableQuery[AccountsTable]

  class OauthClientTable(tag : Tag) extends BaseTable[OAuthClient](tag,"oauth_clients") {
    def ownerId = column[Long]("owner_id")
    def grantType = column[String]("grant_type")
    def clientId = column[String]("client_id")
    def clientSecret = column[String]("client_secret")
    def redirectUri = column[Option[String]]("redirect_uri")
    def * = (id, ownerId, grantType, clientId, clientSecret, redirectUri, createdAt) <> (OAuthClient.tupled, OAuthClient.unapply)

    def owner = foreignKey(
      "oauth_client_account_fk",
      ownerId,
      accountsTableQ)(_.id)
  }

  implicit val OauthClientTableQ : TableQuery[OauthClientTable] = TableQuery[OauthClientTable]

  class OauthAuthorizationCodeTable(tag : Tag) extends BaseTable[OAuthAuthorizationCode](tag,"oauth_authorization_codes") {
    def accountId = column[Long]("account_id")
    def oauthClientId = column[Long]("oauth_client_id")
    def code = column[String]("code")
    def redirectUri = column[Option[String]]("redirect_uri")
    def * = (id, accountId, oauthClientId, code, redirectUri, createdAt) <> (OAuthAuthorizationCode.tupled, OAuthAuthorizationCode.unapply)

    def account = foreignKey(
      "oauth_authorization_code_account_fk",
      accountId,
      accountsTableQ)(_.id)

    def oauthClient = foreignKey(
      "oauth_authorization_code_client_fk",
      oauthClientId,
      OauthClientTableQ)(_.id)
  }

  implicit val OauthAuthorizationCodeTableQ : TableQuery[OauthAuthorizationCodeTable] = TableQuery[OauthAuthorizationCodeTable]

  class OauthAccessTokenTable(tag : Tag) extends BaseTable[OAuthAccessToken](tag,"oauth_access_tokens") {
    def accountId = column[Long]("account_id")
    def oauthClientId = column[Long]("oauth_client_id")
    def accessToken = column[String]("access_token")
    def refreshToken = column[String]("refresh_token")
    def * = (id, accountId, oauthClientId, accessToken, refreshToken, createdAt) <> (OAuthAccessToken.tupled, OAuthAccessToken.unapply)

    def account = foreignKey(
      "oauth_access_token_account_fk",
      accountId,
      accountsTableQ)(_.id)

    def oauthClient = foreignKey(
      "oauth_access_token_client_fk",
      oauthClientId,
      OauthAuthorizationCodeTableQ)(_.id)

  }

  implicit val OauthAccessTokenTableQ : TableQuery[OauthAccessTokenTable] = TableQuery[OauthAccessTokenTable]


}