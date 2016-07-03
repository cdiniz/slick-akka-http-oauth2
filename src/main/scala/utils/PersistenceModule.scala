package utils

import persistence.dals._
import persistence.entities._
import persitence.handlers.OAuth2DataHandler
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scalaoauth2.provider.DataHandler


trait Profile {
	val profile: JdbcProfile
}


trait DbModule extends Profile{
	val db: JdbcProfile#Backend#Database
}

trait PersistenceModule {
	val accountsDal: AccountsDal
	val oauthAuthorizationCodesDal: OAuthAuthorizationCodesDal
	val oauthClientsDal: OAuthClientsDal
	val oauthAccessTokensDal:  OAuthAccessTokensDal
	val oauth2DataHandler : DataHandler[Account]
	def generateDDL : Unit
}


trait PersistenceModuleImpl extends PersistenceModule with DbModule{
	this: Configuration  =>

	// use an alternative database configuration ex:
	// private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("pgdb")
	private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("h2db")

	override implicit val profile: JdbcProfile = dbConfig.driver
	override implicit val db: JdbcProfile#Backend#Database = dbConfig.db

	override val accountsDal = new AccountsDalImpl
	override val oauthAuthorizationCodesDal = new OAuthAuthorizationCodesDalImpl
  override val oauthClientsDal = new OAuthClientsDalImpl(this)
  override val oauthAccessTokensDal = new  OAuthAccessTokensDalImpl(this)
	override val oauth2DataHandler = new OAuth2DataHandler(this)

	override def generateDDL(): Unit = {
		accountsDal.createTable()
		oauthAccessTokensDal.createTable()
		oauthAuthorizationCodesDal.createTable()
		oauthClientsDal.createTable()
	}

}