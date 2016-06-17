package persistence.entities

case class Supplier(id: Long, name: String, desc: String) extends BaseEntity

case class SimpleSupplier(name: String, desc: String)