import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import rest.OAuthRoutes
import utils._

object Main extends App with RouteConcatenation {
  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl  with ActorModuleImpl with PersistenceModuleImpl
  implicit val system = modules.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = modules.system.dispatcher

  val bindingFuture = Http().bindAndHandle(
    new OAuthRoutes(modules).routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

}