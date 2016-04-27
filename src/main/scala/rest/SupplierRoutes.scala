package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import entities.JsonProtocol
import persistence.entities.{SimpleSupplier, Supplier}
import utils.{Configuration, PersistenceModule}
import JsonProtocol._
import SprayJsonSupport._
import scala.util.{Failure, Success}
import io.swagger.annotations._
import javax.ws.rs.Path

@Path("/supplier")
@Api(value = "/supplier", produces = "application/json")
class SupplierRoutes(modules: Configuration with PersistenceModule)  extends Directives {

  @Path("/{id}")
  @ApiOperation(value = "Return Supplier", notes = "", nickname = "", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "Supplier Id", required = false, dataType = "int", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return Supplier", response = classOf[Supplier]),
    new ApiResponse(code = 404, message = "Return Supplier Not Found"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def supplierGetRoute = path("supplier" / IntNumber) { (supId) =>
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

  @ApiOperation(value = "Add Supplier", notes = "", nickname = "", httpMethod = "POST", produces = "text/plain")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Supplier Object", required = true,
      dataType = "persistence.entities.SimpleSupplier", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal server error"),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created")
  ))
 def supplierPostRoute = path("supplier") {
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

