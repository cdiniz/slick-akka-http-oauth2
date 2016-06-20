package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import persistence.entities.{SimpleSupplier, Supplier}
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes._
import persistence.entities.JsonProtocol._
import SprayJsonSupport._
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.server.ValidationRejection

class RoutesSpec extends AbstractRestTest {

  def actorRefFactory = system
  val modules = new Modules {}
  val suppliers = new SupplierRoutes(modules)
  val oauthRoutes = new OAuthRoutes(modules)

  "OAuth Routes" should {
    "return unauthorized when trying to get a token without any credentials" in {
      Post("/oauth/access_token") ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual Unauthorized

      }
    }

    "return Ok when trying to get a token with valid credentials" in {

      modules.oauthClientsDal.validate("bob_client_id","bob_client_secret","client_credentials") returns (Future(true))

      Post("/oauth/access_token",FormData("client_id" -> "bob_client_id",
        "client_secret" -> "bob_client_secret", "grant_type" -> "client_credentials")) ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
      }

    }

    "return unauthorized when trying to access resources without token" in {
      Get("/resources") ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual Unauthorized
      }
    }
  }

  "Supplier Routes" should {

    "return an empty array of suppliers" in {
     modules.suppliersDal.findById(1) returns Future(None)

      Get("/supplier/1") ~> suppliers.routes ~> check {
        handled shouldEqual true
        status shouldEqual NotFound
      }
    }

    "return an empty array of suppliers when ask for supplier Bad Request when the supplier is < 1" in {
      Get("/supplier/0") ~> suppliers.routes ~> check {
        handled shouldEqual false
        rejection shouldEqual ValidationRejection("The supplier id should be greater than zero", None)
      }
    }

    "return an array with 1 suppliers" in {
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