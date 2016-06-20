package rest

import org.scalatest.{Matchers, WordSpec}
import org.specs2.mock.Mockito
import persistence.dal._
import persistence.entities.Supplier
import utils.{ActorModule, ConfigurationModuleImpl, PersistenceModule}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.testkit.ScalatestRouteTest
import persistence.entities.SlickTables.SuppliersTable

trait AbstractRestTest  extends WordSpec with Matchers with ScalatestRouteTest with Mockito{

  trait Modules extends ConfigurationModuleImpl with ActorModule with PersistenceModule {
    val system = AbstractRestTest.this.system

    override val suppliersDal = mock[BaseDal[SuppliersTable,Supplier]]
    override val accountsDal =  mock[AccountsDal]
    override val oauthAuthorizationCodesDal = mock[OAuthAuthorizationCodesDal]
    override val oauthClientsDal = mock[OAuthClientsDal]
    override val oauthAccessTokensDal = mock[OAuthAccessTokensDal]
    override def config = getConfig.withFallback(super.config)
  }

  def getConfig: Config = ConfigFactory.empty();


}
