package persistence.entities

import slick.driver.H2Driver.api._
import slick.lifted.Tag
import java.sql.{Timestamp}

abstract class BaseTable[T](tag: Tag, name: String) extends Table[T](tag, name) {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def createdAt = column[Timestamp]("created_at")
}