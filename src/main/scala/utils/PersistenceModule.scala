package utils

import akka.actor.{ActorPath, ActorSelection, Props, ActorRef}
import persistence.dal._
import slick.backend.DatabaseConfig
import slick.driver.{JdbcProfile}
import persistence.entities.{SuppliersTable, Supplier}
import slick.lifted.TableQuery


trait Profile {
	val profile: JdbcProfile
}


trait DbModule extends Profile{
	val db: JdbcProfile#Backend#Database
}

trait PersistenceModule {
	val suppliersDal: BaseDal[SuppliersTable,Supplier]
}


trait PersistenceModuleImpl extends PersistenceModule with DbModule{
	this: Configuration  =>

	// use an alternative database configuration ex:
	// private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("pgdb")
	private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("h2db")

	override implicit val profile: JdbcProfile = dbConfig.driver
	override implicit val db: JdbcProfile#Backend#Database = dbConfig.db

	override val suppliersDal = new BaseDalImpl[SuppliersTable,Supplier](TableQuery[SuppliersTable]) {}

	val self = this

}
