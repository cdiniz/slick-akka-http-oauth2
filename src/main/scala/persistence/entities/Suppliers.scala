package persistence.entities

import slick.driver.H2Driver.api._

case class Supplier(id: Long, name: String, desc: String) extends BaseEntity

case class SimpleSupplier(name: String, desc: String)

class SuppliersTable(tag: Tag) extends BaseTable[Supplier](tag, "SUPPLIERS") {
  def name = column[String]("userID")
  def desc = column[String]("last_name")
  def * = (id, name, desc) <> (Supplier.tupled, Supplier.unapply)
}