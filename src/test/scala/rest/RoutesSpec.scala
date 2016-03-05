package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import entities.JsonProtocol
import persistence.entities.{SimpleSupplier, Supplier}
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes._
import JsonProtocol._
import SprayJsonSupport._

class RoutesSpec extends AbstractRestTest {

  def actorRefFactory = system
  val modules = new Modules {}
  val suppliers = new SupplierRoutes(modules)

  "Supplier Routes" should {

    "return an empty array of suppliers" in {
     modules.suppliersDal.findById(1) returns Future(None)

      Get("/supplier/1") ~> suppliers.routes ~> check {
        handled shouldEqual true
        status shouldEqual NotFound
      }
    }

    "return an array with 2 suppliers" in {
      modules.suppliersDal.findById(1) returns Future(Some(Supplier(1,"name 1", "desc 1")))
      Get("/supplier/1") ~> suppliers.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
        responseAs[Option[Supplier]].isEmpty shouldEqual false
      }
    }

    "create a supplier with the json in post" in {
      modules.suppliersDal.insert(Supplier(0,"name 1","desc 1")) returns  Future(1)
      Post("/supplier",SimpleSupplier("name 1","desc 1")) ~> suppliers.routes ~> check {
        handled shouldEqual true
        status shouldEqual Created
      }
    }

    "not handle the invalid json" in {
      Post("/supplier","{\"name\":\"1\"}") ~> suppliers.routes ~> check {
        handled shouldEqual false
      }
    }

    "not handle an empty post" in {
      Post("/supplier") ~> suppliers.routes ~> check {
        handled shouldEqual false
      }
    }

  }

}