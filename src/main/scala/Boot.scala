import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import rest.{OAuthRoutes, SupplierRoutes}
import utils._

object Main extends App with RouteConcatenation with CorsSupport{
  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl  with ActorModuleImpl with PersistenceModuleImpl
  implicit val system = modules.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = modules.system.dispatcher

  modules.suppliersDal.createTable()

  val swaggerService = new SwaggerDocService(system)

  val bindingFuture = Http().bindAndHandle(
    new SupplierRoutes(modules).routes ~
    new OAuthRoutes(modules).routes ~
    swaggerService.assets ~
    corsHandler(swaggerService.routes), "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

}