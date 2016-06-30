package persistence.dals

import akka.util.Timeout
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import persistence.entities.Supplier

import scala.concurrent.Await
import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class SuppliersDALTest extends FunSuite with AbstractPersistenceTest with BeforeAndAfterAll{
  implicit val timeout = Timeout(5.seconds)

  val modules = new Modules {
  }

  test("SuppliersActor: Testing Suppliers DAL") {
    Await.result(modules.suppliersDal.createTable(),5.seconds)
    val numberOfEntities : Long = Await.result((modules.suppliersDal.insert(Supplier(0,"sup","desc"))),5.seconds)
    assert (numberOfEntities == 1)
    val supplier : Option[Supplier] = Await.result((modules.suppliersDal.findById(1)),5.seconds)
    assert (! supplier.isEmpty &&  supplier.get.name.compareTo("sup") == 0)
    val empty : Option[Supplier] = Await.result((modules.suppliersDal.findById(2)),5.seconds)
    assert (empty.isEmpty)
  }

  override def afterAll: Unit ={
    modules.db.close()
  }
}