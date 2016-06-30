package rest

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.Credentials
import rest.OAuth2RouteProvider.TokenResponse
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaoauth2.provider._

trait OAuth2RouteProvider[U] extends Directives with DefaultJsonProtocol{
  import OAuth2RouteProvider.tokenResponseFormat
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  val oauth2DataHandler : DataHandler[U]


  val tokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials,
      OAuthGrantType.PASSWORD -> new Password,
      OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode,
      OAuthGrantType.REFRESH_TOKEN -> new RefreshToken
    )
  }

  def grantResultToTokenResponse(grantResult : GrantHandlerResult[U]) : TokenResponse =
    TokenResponse(grantResult.tokenType, grantResult.accessToken, grantResult.expiresIn.getOrElse(1L), grantResult.refreshToken.getOrElse(""))

  def oauth2Authenticator(credentials: Credentials): Future[Option[AuthInfo[U]]] =
    credentials match {
      case p@Credentials.Provided(token) =>
        oauth2DataHandler.findAccessToken(token).flatMap {
          case Some(token) => oauth2DataHandler.findAuthInfoByAccessToken(token)
          case None => Future.successful(None)
        }
      case _ => Future.successful(None)
    }

  def accessTokenRoute = pathPrefix("oauth") {
    path("access_token") {
      post {
        formFieldMap { fields =>
          onComplete(tokenEndpoint.handleRequest(new AuthorizationRequest(Map(), fields.map(m => m._1 -> Seq(m._2))), oauth2DataHandler)) {
            case Success(maybeGrantResponse) =>
              maybeGrantResponse.fold(oauthError => complete(Unauthorized),
                grantResult => complete(tokenResponseFormat.write(grantResultToTokenResponse(grantResult)))
              )
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }

}

object OAuth2RouteProvider extends DefaultJsonProtocol{
  case class TokenResponse(token_type : String, access_token : String, expires_in : Long, refresh_token : String)
  implicit val tokenResponseFormat = jsonFormat4(TokenResponse)
}