package rest

import org.scalatest.{Matchers, WordSpec}
import org.specs2.mock.Mockito
import persistence.dal.BaseDal
import persistence.entities.{Supplier, SuppliersTable}
import utils.{PersistenceModule, ConfigurationModuleImpl, ActorModule}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.testkit.ScalatestRouteTest

trait AbstractRestTest  extends WordSpec with Matchers with ScalatestRouteTest with Mockito{

  trait Modules extends ConfigurationModuleImpl with ActorModule with PersistenceModule {
    val system = AbstractRestTest.this.system

    override val suppliersDal = mock[BaseDal[SuppliersTable,Supplier]]

    override def config = getConfig.withFallback(super.config)
  }

  def getConfig: Config = ConfigFactory.empty();


}
