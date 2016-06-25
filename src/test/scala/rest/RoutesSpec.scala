package rest

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import persistence.entities._
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes._
import persistence.entities.JsonProtocol._
import SprayJsonSupport._
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.server.ValidationRejection
import org.joda.time.DateTime

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

    "return Ok and a token when trying to get a token with valid credentials" in {
      val bobUser = Account(1,"bobmail@gmail.com","pass",new Timestamp(new DateTime().getMillis))
      val bobToken = OAuthAccessToken(1, 1, 1, "valid token", "refresh token", new Timestamp(new DateTime().getMillis))

      modules.oauthClientsDal.validate("bob_client_id","bob_client_secret","client_credentials") returns (Future(true))
      modules.oauthClientsDal.findClientCredentials("bob_client_id","bob_client_secret") returns Future(Some(bobUser))
      modules.oauthAccessTokensDal.findByAuthorized(bobUser, "bob_client_id") returns Future(Some(bobToken))

      Post("/oauth/access_token",FormData("client_id" -> "bob_client_id",
        "client_secret" -> "bob_client_secret", "grant_type" -> "client_credentials")) ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
        val response = responseAs[TokenResponse]
        response.access_token shouldEqual "valid token"
        response.refresh_token shouldEqual "refresh token"
        response.token_type shouldEqual "Bearer"
        response.expires_in shouldEqual 3599
      }

    }

    "return Ok and a token when trying to get a token with valid password" in {
      val bobUser = Account(1,"bobmail@gmail.com","pass",new Timestamp(new DateTime().getMillis))
      val bobToken = OAuthAccessToken(1, 1, 1, "valid token", "refresh token", new Timestamp(new DateTime().getMillis))

      modules.oauthClientsDal.validate("bob_client_id","bob_client_secret","password") returns (Future(true))
      modules.accountsDal.authenticate("bobmail@gmail.com","pass") returns Future(Some(bobUser))
      modules.oauthAccessTokensDal.findByAuthorized(bobUser, "bob_client_id") returns Future(Some(bobToken))

      Post("/oauth/access_token",FormData("client_id" -> "bob_client_id",
        "client_secret" -> "bob_client_secret","username" -> "bobmail@gmail.com", "password" -> "pass", "grant_type" -> "password")) ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
        val response = responseAs[TokenResponse]
        response.access_token shouldEqual "valid token"
        response.refresh_token shouldEqual "refresh token"
        response.token_type shouldEqual "Bearer"
        response.expires_in shouldEqual 3599
      }

    }

    "return Ok and a token when trying to get a token with valid authorization code" in {
      val bobUser = Account(1,"bobmail@gmail.com","pass",new Timestamp(new DateTime().getMillis))
      val bobToken = OAuthAccessToken(1, 1, 1, "valid token", "refresh token", new Timestamp(new DateTime().getMillis))

      modules.oauthClientsDal.validate("bob_client_id","bob_client_secret","authorization_code") returns (Future(true))
      modules.oauthAuthorizationCodesDal.findByCode("bob_code") returns Future(Some(OAuthAuthorizationCode(1,1,1,"bob_code",Some("http://localhost:3000/callback"),new Timestamp(DateTime.now().getMillis))))
      modules.accountsDal.findByAccountId(1) returns Future(Some(bobUser))
      modules.oauthClientsDal.findByClientId(1) returns Future(Some(OAuthClient(1,1,"authorization_code","bob_client_id","bob_client_secret",Some("http://localhost:3000/callback"),new Timestamp(DateTime.now().getMillis))))

      modules.oauthAccessTokensDal.findByAuthorized(bobUser, "bob_client_id") returns Future(Some(bobToken))

      Post("/oauth/access_token",FormData("client_id" -> "bob_client_id",
        "client_secret" -> "bob_client_secret","redirect_uri" -> "http://localhost:3000/callback", "code" -> "bob_code", "grant_type" -> "authorization_code")) ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
        val response = responseAs[TokenResponse]
        response.access_token shouldEqual "valid token"
        response.refresh_token shouldEqual "refresh token"
        response.token_type shouldEqual "Bearer"
        response.expires_in shouldEqual 3599
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