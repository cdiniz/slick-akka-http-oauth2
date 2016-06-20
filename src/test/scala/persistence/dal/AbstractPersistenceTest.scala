package persistence.dal


import akka.actor.{ActorSystem, Props}
import persistence.entities.Supplier
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import utils._
import org.scalatest.{FunSuite, Suite}
import persistence.entities.SlickTables.SuppliersTable

trait AbstractPersistenceTest extends FunSuite {  this: Suite =>


  trait Modules extends ConfigurationModuleImpl  with PersistenceModuleTest {
  }


  trait PersistenceModuleTest extends PersistenceModule with DbModule{
    this: Configuration  =>

    private val dbConfig : DatabaseConfig[JdbcProfile]  = DatabaseConfig.forConfig("h2test")

    override implicit val profile: JdbcProfile = dbConfig.driver
    override implicit val db: JdbcProfile#Backend#Database = dbConfig.db

    override val suppliersDal = new BaseDalImpl[SuppliersTable,Supplier]
    override val accountsDal = new AccountsDalImpl
    override val oauthAuthorizationCodesDal = new OAuthAuthorizationCodesDalImpl
    override val oauthClientsDal = new OAuthClientsDalImpl(this)
    override val oauthAccessTokensDal = new  OAuthAccessTokensDalImpl(this)

    val self = this

  }

}