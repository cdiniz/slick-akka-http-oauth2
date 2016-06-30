package rest

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import persistence.entities._
import utils.{Configuration, PersistenceModule}

import scalaoauth2.provider._

class OAuthRoutes(val modules: Configuration with PersistenceModule)  extends Directives with OAuth2RouteProvider[Account] {

  override val oauth2DataHandler = modules.oauth2DataHandler

  def protectedRoute = path("resources") {
    get {
      authenticateOAuth2Async[AuthInfo[Account]]("realm", oauth2Authenticator) {
        auth => complete(OK,s"Hello ${auth.clientId.getOrElse("")}")
      }
    }
  }

  val routes: Route = accessTokenRoute ~ protectedRoute

}
