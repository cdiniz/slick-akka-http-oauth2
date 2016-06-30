package rest

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import utils.{Configuration, PersistenceModule}

import scala.concurrent.Future
import scalaoauth2.provider._
import persistence.entities._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.directives.Credentials
import persistence.entities.JsonProtocol.tokenResponseFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

class OAuthRoutes(val modules: Configuration with PersistenceModule)  extends Directives with OAuth2RouteUtils {

  def accessTokenRoute = pathPrefix("oauth") {
    path("access_token") {
      post {
        formFieldMap { fields =>
          onComplete(tokenEndpoint.handleRequest(new AuthorizationRequest(Map(), fields.map(m => m._1 -> Seq(m._2))), oauth2DateHandler)) {
            case Success(maybeGrantResponse) =>
              maybeGrantResponse.fold(oauthError => complete(Unauthorized),
                grantResult => complete(grantResultToTokenResponse(grantResult))
              )
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }

  def protectedRoute = path("resources") {
    get {
      authenticateOAuth2Async[AuthInfo[Account]]("realm", oauth2Authenticator) {
        auth => complete(OK,s"Hello ${auth.clientId.getOrElse("")}")
      }
    }
  }

  val routes: Route = accessTokenRoute ~ protectedRoute

}
