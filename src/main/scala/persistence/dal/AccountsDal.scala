package persistence.dal

import java.security.MessageDigest

import persistence.entities.SlickTables.AccountsTable
import persistence.entities.Account
import slick.driver.H2Driver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.JdbcProfile

trait AccountsDal extends BaseDalImpl[AccountsTable,Account] {
  def authenticate(email: String, password: String): Future[Option[Account]]
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