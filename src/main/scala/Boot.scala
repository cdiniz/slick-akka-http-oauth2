import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import rest.SupplierRoutes
import utils.{PersistenceModuleImpl, ActorModuleImpl, ConfigurationModuleImpl}

object Main extends App {
  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl  with ActorModuleImpl with PersistenceModuleImpl
  implicit val system = modules.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = modules.system.dispatcher

  modules.suppliersDal.createTable()

  val bindingFuture = Http().bindAndHandle(new SupplierRoutes(modules).routes, "localhost", 8080)
 
  println(s"Server online at http://localhost:8080/")

}