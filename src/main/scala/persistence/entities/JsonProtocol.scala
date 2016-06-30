package persistence.entities

import spray.json.DefaultJsonProtocol

object JsonProtocol extends DefaultJsonProtocol {
  implicit val supplierFormat = jsonFormat3(Supplier)
  implicit val simpleSupplierFormat = jsonFormat2(SimpleSupplier)
}