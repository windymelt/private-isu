package com.isu.scala

import org.scalatra._
import scalikejdbc._

import model.{User, Post, PostResult, Comment, CommentResult}
import Config._
import Types._

class MyScalatraServlet
    extends ScalatraServlet
    with XsrfTokenSupport
    with FlashMapSupport
    with DBService {

  def makePostResults(
      posts: Seq[Post],
      allComments: Boolean = false
  ): Seq[PostResult] = {
    import Comment.c, User.u

    def loop(current: Seq[Post], acc: Vector[PostResult], count: Int)(implicit
        session: DBSession
    ): Vector[PostResult] = current match {

      case head +: tail =>
        if (count >= conf.getInt("app.tuning.postsPerPage")) {
          acc
        } else {
          val commentCount =
            sql"SELECT COUNT(*) AS `count` FROM `comments` WHERE `post_id` = ${head.id}"
              .map(_.int(1))
              .first()
              .apply()
              .getOrElse(0)

          val comments =
            sql"SELECT ${c.resultAll} FROM ${Comment as c} WHERE ${c.postId} = ${head.id} ORDER BY ${c.createdAt} DESC ${if (allComments) sqls.empty
            else sqls"LIMIT 3"}".map(Comment(_)).list().apply()
          val commentResults = comments.map { comment =>
            val Some(user) =
              sql"SELECT ${u.resultAll} FROM ${User as u} WHERE ${u.id} = ${comment.userId}"
                .map(User(_))
                .first()
                .apply()
            CommentResult(comment, user)
          }.reverse

          val Some(user) =
            sql"SELECT ${u.resultAll} FROM ${User as u} WHERE ${u.id} = ${head.userId}"
              .map(User(_))
              .first()
              .apply()
          if (!user.delFlg) {
            loop(
              tail,
              acc :+ PostResult(head, commentCount, commentResults, user),
              count + 1
            )
          } else {
            loop(tail, acc, count)
          }
        }
      case _ =>
        acc
    }

    DB readOnly { implicit session =>
      loop(posts, Vector.empty, 0)
    }
  }

  def getSessionUser(): Option[User] = {
    import User.u
    DB readOnly { implicit dbsession =>
      session.getAs[Long]("user").flatMap { id =>
        sql"SELECT ${u.resultAll} FROM ${User.as(u)} WHERE ${u.id} = ${id}"
          .map(User(_))
          .first()
          .apply()
      }
    }
  }

  xsrfGuard("/")
  get("/") {
    import Post.p

    implicit val f = flash
    implicit val xkey: String @@ XSRFKey = Tag[XSRFKey](xsrfKey)
    implicit val xtoken: String @@ XSRFToken = Tag[XSRFToken](xsrfToken)

    DB readOnly { implicit dbsession =>
      val posts =
        sql"SELECT ${p.result.id}, ${p.result.userId}, ${p.result.body}, ${p.result.createdAt}, ${p.result.mime} FROM ${Post as p} ORDER BY ${p.createdAt} DESC"
          .map(Post.withoutImage)
          .list()
          .apply()
      Ok(views.html.index(getSessionUser(), makePostResults(posts)))
    }
  }

}