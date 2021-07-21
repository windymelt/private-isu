package com.isu.scala.model

import com.github.nscala_time.time.Imports._
import scalikejdbc._

import DateTimeBinder._

case class Comment(
  id: Int,
  postId: Int,
  userId: Int,
  comment: String,
  createdAt: DateTime
)

case class CommentResult(
  comment: Comment,
  user: User
)

object Comment extends SQLSyntaxSupport[Comment] {
  override val tableName: String = "comments"
  val c: SyntaxProvider[Comment] = this.syntax("c")

  def apply(rs: WrappedResultSet): Comment = autoConstruct[Comment](rs, c.resultName)
}