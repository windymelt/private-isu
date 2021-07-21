package com.isu.scala.model

import scalikejdbc._
import com.github.nscala_time.time.Imports._

import DateTimeBinder._

final case class User(
    id: Int,
    accountName: String,
    passhash: String,
    authority: Boolean,
    delFlg: Boolean,
    createdAt: DateTime
)

object User extends SQLSyntaxSupport[User] {
  override val tableName: String = "users"
  val u: SyntaxProvider[User] = this.syntax("u")

  def apply(rs: WrappedResultSet): User = autoConstruct[User](rs, u.resultName)
}