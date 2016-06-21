package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import utils.{Configuration, PersistenceModule}

import scala.concurrent.Future
import scalaoauth2.provider._
import persistence.entities._
import spray.json.{JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

class OAuthRoutes(modules: Configuration with PersistenceModule)  extends Directives {

  val routes: Route = oauthRoute ~ protectedResourcesRoute
  val oauthDateHandler = new MyDataHandler();
  val tokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials()
    )
  }

  def oauthRoute = pathPrefix("oauth") {
    path("access_token") {
      post {
        formFieldMap { fields =>
          onComplete((tokenEndpoint.handleRequest(
            new AuthorizationRequest(Map(),fields.map(m => m._1 -> Seq(m._2))),oauthDateHandler)
            )) {
            response =>
              response match {
                case Success(maybeGrantResponse) =>
                  maybeGrantResponse.fold(oauthError => complete(Unauthorized),
                    grantResult => complete(JsObject(
                      "token" -> JsString(grantResult.accessToken)
                    ))
                  )
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
          }
        }
      }
    }
  }


def protectedResourcesRoute = path("resources") {
  get {
  complete(Unauthorized)
}
}


  class MyDataHandler extends DataHandler[Account] {

    override def validateClient(request: AuthorizationRequest): Future[Boolean] =  {
      request.clientCredential.fold(Future.successful(false))(clientCredential => modules.oauthClientsDal.validate(clientCredential.clientId,
        clientCredential.clientSecret.getOrElse(""), request.grantType))
    }

    override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = {
      modules.oauthAccessTokensDal.findByAuthorized(authInfo.user, authInfo.clientId.getOrElse("")).map(_.map(toAccessToken))
    }

    private val accessTokenExpireSeconds = 3600

    private def toAccessToken(accessToken: OAuthAccessToken) = {
      AccessToken(
        accessToken.accessToken,
        Some(accessToken.refreshToken),
        None,
        Some(accessTokenExpireSeconds),
        accessToken.createdAt
      )
    }

    override def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken] = {
      authInfo.clientId.fold(Future.failed[AccessToken](new InvalidRequest())) { clientId =>
        (for {
          clientOpt <- modules.oauthClientsDal.findByClientId(clientId)
          toAccessToken <- modules.oauthAccessTokensDal.create(authInfo.user, clientOpt.get).map(toAccessToken) if clientOpt.isDefined
        } yield toAccessToken).recover { case _ => throw new InvalidRequest() }
      }
    }


    override def findUser(request: AuthorizationRequest): Future[Option[Account]] =
      request match {
        case request: PasswordRequest =>
          modules.accountsDal.authenticate(request.username, request.password)
        case request: ClientCredentialsRequest =>
          request.clientCredential.fold(Future.failed[Option[Account]](new InvalidRequest())) { clientCredential =>
            for {
              maybeAccount <- modules.oauthClientsDal.findClientCredentials(
                clientCredential.clientId,
                clientCredential.clientSecret.getOrElse("")
              )
            } yield maybeAccount
          }
        case _ =>
          Future.successful(None)
      }

    override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = {
      modules.oauthAccessTokensDal.findByRefreshToken(refreshToken).flatMap {
        case Some(accessToken) =>
          for {
            account <- modules.accountsDal.findById(accessToken.accountId)
            client <- modules.oauthClientsDal.findById(accessToken.oauthClientId)
          } yield {
            Some(AuthInfo(
              user = account.get,
              clientId = Some(client.get.clientId),
              scope = None,
              redirectUri = None
            ))
          }
        case None => Future.failed(new InvalidRequest())
      }
    }

    override def refreshAccessToken(authInfo: AuthInfo[Account], refreshToken: String): Future[AccessToken] = {
      authInfo.clientId.fold(Future.failed[AccessToken](new InvalidRequest())) { clientId => (for {
        clientOpt <- modules.oauthClientsDal.findByClientId(clientId)
        toAccessToken <- modules.oauthAccessTokensDal.refresh(authInfo.user, clientOpt.get).map(toAccessToken) if clientOpt.isDefined
      } yield toAccessToken).recover { case _ => throw new InvalidClient() }
      }
    }

    override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[Account]]] = {
      modules.oauthAuthorizationCodesDal.findByCode(code).flatMap {
        case Some(code) =>
          for {
            account <- modules.accountsDal.findById(code.accountId)
            client <- modules.oauthClientsDal.findById(code.oauthClientId)
          } yield {
            Some(AuthInfo(
              user = account.get,
              clientId = Some(client.get.clientId),
              scope = None,
              redirectUri = None
            ))
          }
        case None => Future.failed(new InvalidRequest())
      }
    }

    override def deleteAuthCode(code: String): Future[Unit] = modules.oauthAuthorizationCodesDal.delete(code).map(_ => {})

    override def findAccessToken(token: String): Future[Option[AccessToken]] =
      modules.oauthAccessTokensDal.findByAccessToken(token).map(_.map(toAccessToken))

    override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[Account]]] = {
      modules.oauthAccessTokensDal.findByAccessToken(accessToken.token).flatMap {
        case Some(accessToken) =>
          for {
            account <- modules.accountsDal.findById(accessToken.accountId)
            client <- modules.oauthClientsDal.findById(accessToken.oauthClientId)
          } yield {
            Some(AuthInfo(
              user = account.get,
              clientId = Some(client.get.clientId),
              scope = None,
              redirectUri = None
            ))
          }
        case None => Future.failed(new InvalidRequest())
      }
    }

  }

}
