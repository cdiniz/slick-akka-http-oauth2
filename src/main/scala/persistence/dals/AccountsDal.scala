package persistence.dals

import java.security.MessageDigest

import persistence.entities.Account
import persistence.entities.SlickTables.AccountsTable
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AccountsDal extends BaseDalImpl[AccountsTable,Account] {
  def authenticate(email: String, password: String): Future[Option[Account]]
  def findByAccountId(id : Long) : Future[Option[Account]] = findById(id)
}

class AccountsDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends AccountsDal {
  private def digestString(s: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(s.getBytes)
    md.digest.foldLeft("") { (s, b) =>
      s + "%02x".format(if (b < 0) b + 256 else b)
    }
  }
  def authenticate(email: String, password: String): Future[Option[Account]] = {
    val hashedPassword = digestString(password)
    findByFilter( acc => acc.password === hashedPassword && acc.email === email).map(_.headOption)
  }
}