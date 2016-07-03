package rest

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{FormData, HttpEntity}
import org.joda.time.DateTime
import persistence.entities._
import rest.OAuth2RouteProvider.TokenResponse

import scala.concurrent.Future

class RoutesSpec extends AbstractRestTest {

  def actorRefFactory = system
  val modules = new Modules {
    override def generateDDL: Unit = {}
  }
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
      modules.oauthAuthorizationCodesDal.delete("bob_code") returns Future.successful(1)

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

    "return new token after refresh" in {
      val bobUser = Account(1,"bobmail@gmail.com","pass",new Timestamp(new DateTime().getMillis))
      val bobClient = OAuthClient(1,1,"authorization_code","bob_client_id","bob_client_secret",Some("http://localhost:3000/callback"),new Timestamp(DateTime.now().getMillis))
      val bobToken = OAuthAccessToken(1, 1, 1, "valid token", "refresh token", new Timestamp(new DateTime().getMillis))

      modules.oauthClientsDal.validate("bob_client_id","bob_client_secret","refresh_token") returns (Future(true))
      modules.oauthAccessTokensDal.findByRefreshToken("refresh token") returns Future(Some(bobToken))
      modules.accountsDal.findByAccountId(1) returns Future(Some(bobUser))
      modules.oauthClientsDal.findByClientId(1) returns Future(Some(bobClient))
      modules.oauthClientsDal.findByClientId("bob_client_id") returns Future(Some(bobClient))
      modules.oauthAccessTokensDal.refresh(bobUser, bobClient) returns Future(bobToken)

      Post("/oauth/access_token",FormData("client_id" -> "bob_client_id", "client_secret" -> "bob_client_secret",
        "refresh_token" -> "refresh token", "grant_type" -> "refresh_token")) ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
        val response = responseAs[TokenResponse]
        response.access_token shouldEqual "valid token"
        response.refresh_token shouldEqual "refresh token"
        response.token_type shouldEqual "Bearer"
        response.expires_in shouldEqual 3599
      }
    }

    "don't handle when trying to access resources without token" in {
      modules.oauthAccessTokensDal.findByAccessToken("") returns Future(None)

      Get("/resources") ~> oauthRoutes.routes ~> check {
        handled shouldEqual false
      }
    }

    "return unauthorized when trying to access resources without token" in {
      modules.oauthAccessTokensDal.findByAccessToken("invalid") returns Future(None)

      Get("/resources").addHeader(Authorization(OAuth2BearerToken("invalid"))) ~> oauthRoutes.routes ~> check {
        handled shouldEqual false
      }
    }

    "return authorized when trying to access resources with a valid token" in {
      val bobToken = OAuthAccessToken(1, 1, 1, "valid token", "refresh token", new Timestamp(new DateTime().getMillis))
      val bobUser = Account(1,"bobmail@gmail.com","pass",new Timestamp(new DateTime().getMillis))
      val bobClient = OAuthClient(1,1,"authorization_code","bob_client_id","bob_client_secret",Some("http://localhost:3000/callback"),new Timestamp(DateTime.now().getMillis))

      modules.oauthAccessTokensDal.findByAccessToken("valid token") returns Future(Some(bobToken))

      modules.accountsDal.findByAccountId(1) returns Future(Some(bobUser))
      modules.oauthClientsDal.findByClientId(1) returns Future(Some(bobClient))

      Get("/resources",HttpEntity("Application/json")).addHeader(Authorization(OAuth2BearerToken("valid token"))) ~> oauthRoutes.routes ~> check {
        handled shouldEqual true
        status shouldEqual OK
        responseAs[String] should equal("Hello bob_client_id")
      }
    }

  }

}