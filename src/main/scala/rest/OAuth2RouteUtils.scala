package rest

import akka.http.scaladsl.server.directives.Credentials
import persistence.entities.{Account, OAuthAccessToken, TokenResponse}
import utils.{Configuration, PersistenceModule}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaoauth2.provider.{ClientCredentialsRequest, InvalidClient, PasswordRequest, _}

trait OAuth2RouteUtils {
  val modules: Configuration with PersistenceModule

  val tokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials,
      OAuthGrantType.PASSWORD -> new Password,
      OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode,
      OAuthGrantType.REFRESH_TOKEN -> new RefreshToken
    )
  }

  val oauth2DateHandler = new Oauth2DataHandler

  def oauth2Authenticator(credentials: Credentials): Future[Option[AuthInfo[Account]]] =
    credentials match {
      case p@Credentials.Provided(token) =>
        oauth2DateHandler.findAccessToken(token).flatMap {
          case Some(token) => oauth2DateHandler.findAuthInfoByAccessToken(token)
          case None => Future.successful(None)
        }
      case _ => Future.successful(None)
    }

  def grantResultToTokenResponse(grantResult : GrantHandlerResult[Account]) : TokenResponse =
    TokenResponse(grantResult.tokenType, grantResult.accessToken, grantResult.expiresIn.getOrElse(1L), grantResult.refreshToken.getOrElse(""))

  class Oauth2DataHandler extends DataHandler[Account] {

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
            account <- modules.accountsDal.findByAccountId(accessToken.accountId)
            client <- modules.oauthClientsDal.findByClientId(accessToken.oauthClientId)
          } yield {
            Some(AuthInfo(
              user = account.get,
              clientId = Some(client.get.clientId),
              scope = None,
              redirectUri = client.get.redirectUri
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
            account <- modules.accountsDal.findByAccountId(code.accountId)
            client <- modules.oauthClientsDal.findByClientId(code.oauthClientId)
          } yield {
            Some(AuthInfo(
              user = account.get,
              clientId = Some(client.get.clientId),
              scope = None,
              redirectUri = client.get.redirectUri
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
            account <- modules.accountsDal.findByAccountId(accessToken.accountId)
            client <- modules.oauthClientsDal.findByClientId(accessToken.oauthClientId)
          } yield {
            Some(AuthInfo(
              user = account.get,
              clientId = Some(client.get.clientId),
              scope = None,
              redirectUri = client.get.redirectUri
            ))
          }
        case None => Future.failed(new InvalidRequest())
      }
    }

  }
}