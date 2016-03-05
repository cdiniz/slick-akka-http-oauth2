package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import entities.JsonProtocol
import persistence.entities.{SimpleSupplier, Supplier}
import utils.{PersistenceModule, Configuration}
import JsonProtocol._
import SprayJsonSupport._
import scala.util.{Failure, Success}

class SupplierRoutes(modules: Configuration with PersistenceModule) {

  private val supplierGetRoute = path("supplier" / IntNumber) { (supId) =>
    get {
      onComplete((modules.suppliersDal.findById(supId)).mapTo[Option[Supplier]]) {
        case Success(supplierOpt) => supplierOpt match {
          case Some(sup) => complete(sup)
          case None => complete(NotFound, s"The supplier doesn't exist")
        }
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }

  private val supplierPostRoute = path("supplier") {
    post {
      entity(as[SimpleSupplier]) { supplierToInsert => onComplete((modules.suppliersDal.insert(Supplier(0, supplierToInsert.name, supplierToInsert.desc)))) {
        // ignoring the number of insertedEntities because in this case it should always be one, you might check this in other cases
        case Success(insertedEntities) => complete(Created)
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
      }
    }
  }

  val routes: Route = supplierPostRoute ~ supplierGetRoute

}

