import java.sql.Timestamp

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import org.joda.time.DateTime
import persistence.entities.{Account, OAuthClient}
import rest.OAuthRoutes
import utils._

object Main extends App with RouteConcatenation {
  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl  with ActorModuleImpl with PersistenceModuleImpl
  implicit val system = modules.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = modules.system.dispatcher

  modules.generateDDL()

  for {
    createAccounts <- modules.accountsDal.insert(Seq(
      Account(0, "bob@example.com", "48181acd22b3edaebc8a447868a7df7ce629920a", new Timestamp(new DateTime().getMillis)) // password:bob
    ))
    createOauthClients <- modules.oauthClientsDal.insert(Seq(
      OAuthClient(0, 1, "client_credentials", "bob_client_id", "bob_client_secret", Some("redirectUrl"), new Timestamp(new DateTime().getMillis))))
  } yield {
    println(s"Database initialized with default values for bob and alice")
  }

  val bindingFuture = Http().bindAndHandle(
    new OAuthRoutes(modules).routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

}