package rest

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{Matchers, WordSpec}
import org.specs2.mock.Mockito
import persistence.dals._
import persitence.handlers.OAuth2DataHandler
import utils.{ActorModule, ConfigurationModuleImpl, PersistenceModule}

trait AbstractRestTest  extends WordSpec with Matchers with ScalatestRouteTest with Mockito{

  trait Modules extends ConfigurationModuleImpl with ActorModule with PersistenceModule {
    val system = AbstractRestTest.this.system

    override val accountsDal =  mock[AccountsDal]
    override val oauthAuthorizationCodesDal = mock[OAuthAuthorizationCodesDal]
    override val oauthClientsDal = mock[OAuthClientsDal]
    override val oauthAccessTokensDal = mock[OAuthAccessTokensDal]
    override val oauth2DataHandler = new OAuth2DataHandler(this)
    override def config = getConfig.withFallback(super.config)
  }

  def getConfig: Config = ConfigFactory.empty();


}
