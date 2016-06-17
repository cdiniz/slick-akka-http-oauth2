package utils

import akka.actor.{ActorPath, ActorSelection, Props, ActorRef}
import persistence.dal._
import slick.backend.DatabaseConfig
import slick.driver.{JdbcProfile}
import persistence.entities.SlickTables._
import persistence.entities._
import slick.lifted.TableQuery


trait Profile {
	val profile: JdbcProfile
}


trait DbModule extends Profile{
	val db: JdbcProfile#Backend#Database
}

trait PersistenceModule {
	val suppliersDal: BaseDal[SuppliersTable,Supplier]
	val accountsDal: AccountsDal
	val oauthAuthorizationCodesDal: OAuthAuthorizationCodesDal
	val oauthClientsDal: OAuthClientsDal
	val oauthAccessTokensDal:  OAuthAccessTokensDal
}


trait PersistenceModuleImpl extends PersistenceModule with DbModule{
	this: Configuration  =>

	// use an alternative database configuration ex:
	// private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("pgdb")
	private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("h2db")

	override implicit val profile: JdbcProfile = dbConfig.driver
	override implicit val db: JdbcProfile#Backend#Database = dbConfig.db

	override val suppliersDal = new BaseDalImpl[SuppliersTable,Supplier]
	override val accountsDal = new AccountsDalImpl
	override val oauthAuthorizationCodesDal = new OAuthAuthorizationCodesDalImpl
  override val oauthClientsDal = new OAuthClientsDalImpl(this)
  override val oauthAccessTokensDal = new  OAuthAccessTokensDalImpl(this)

}
