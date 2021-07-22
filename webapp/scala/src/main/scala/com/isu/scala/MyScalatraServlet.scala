package com.isu.scala

import org.scalatra._
import org.scalatra.forms.{views => _, _}
import scalikejdbc._

import model.{User, Post, PostResult, Comment, CommentResult}
import Config._
import Types._
import org.scalatra.i18n.I18nSupport

class MyScalatraServlet
    extends ScalatraServlet
    with XsrfTokenSupport
    with FlashMapSupport
    with DBService
    with FormSupport
    with I18nSupport {

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

  // ok GET     /initialize                 initialize()
  // ok GET     /login                      showLogin()
  // ok POST    /login                      newLogin()
  // ok GET     /register                   showRegister()
  // ok POST    /register                   register()
  // ok GET     /logout                     logout()
  // ok GET     /                           index()
  // ok GET     /@:accountName              showAccount(accountName: String)
  // ok GET     /posts                      posts()
  // GET     /posts/:id                  showPost(id: Int)
  // POST    /                           createPost()
  // GET     /image/:id.:ext             showImage(id: Int, ext: String)
  // POST    /comment                    createComment()
  // GET     /admin/banned               banned()
  // POST    /admin/banned               ban()
  // GET     /*file                      AssetsController.at(file)

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

  get("/initialize") {
    DB autoCommit { implicit session =>
      sql"DELETE FROM users WHERE id > 1000".execute().apply()
      sql"DELETE FROM posts WHERE id > 10000".execute().apply()
      sql"DELETE FROM comments WHERE id > 100000".execute().apply()
      sql"UPDATE users SET del_flg = 0".execute().apply()
      sql"UPDATE users SET del_flg = 1 WHERE id % 50 = 0".execute().apply()
    }
    Ok
  }

  get("/login") {
    implicit val f = flash
    getSessionUser match {
      case Some(_) => Found("/")
      case _       => Ok(views.html.login(None))
    }
  }

  case class LoginData(accountName: String, password: String)
  val loginForm = mapping(
    "account_name" -> text(required),
    "password" -> text(required)
  )(LoginData.apply)

  post("/login") {
    getSessionUser match {
      case Some(_) => Found("/")
      case _ =>
        validate(loginForm)(
          (err: Seq[(String, String)]) => BadRequest(err),
          form => {
            import User.u
            DB readOnly { implicit session =>
              sql"SELECT ${u.resultAll} FROM ${User as u} WHERE ${u.accountName} = ${form.accountName} AND ${u.delFlg} = 0"
                .map(User(_))
                .first()
                .apply() match {
                case Some(user: User)
                    if Digest.calculatePasshash(
                      user.accountName,
                      form.password
                    ) == user.passhash => {
                  session("user") = user.id.toString
                  Found("/")
                }
                case _ => {
                  flash("notice") = "アカウント名かパスワードが間違っています"
                  Found("/login")
                }
              }
            }
          }
        )
    }
  }

  get("/register") {
    implicit val f = flash
    getSessionUser match {
      case Some(_) => Found("/")
      case _       => Ok(views.html.register(None))
    }
  }

  case class RegistrationData(accountName: String, password: String)
  val registrationForm = mapping(
    "account_name" -> text(required, minlength(3)),
    "password" -> text(required, minlength(6))
  )(RegistrationData.apply)
  post("/register") {
    DB autoCommit { implicit session =>
      getSessionUser match {
        case Some(_) => Found("/")
        case _ =>
          validate(registrationForm)(
            (err) => {
              flash("notice") = "アカウント名は3文字以上、パスワードは6文字以上である必要があります"
              Found("/register")
            },
            (form) => {
              if (
                sql"SELECT 1 FROM users WHERE `account_name` = ${form.accountName}"
                  .map(_.int(1))
                  .first()
                  .apply()
                  .nonEmpty
              ) {
                flash("notice") = "アカウント名がすでに使われています"
                Found("/register")
              } else {
                val passhash =
                  Digest.calculatePasshash(form.accountName, form.password)
                val id =
                  sql"INSERT INTO `users` (`account_name`, `passhash`) VALUES (${form.accountName}, ${passhash})"
                    .updateAndReturnGeneratedKey()
                    .apply()

                session("user") = id.toString
                Found("/")
              }
            }
          )
      }
    }
  }

  get("/logout") {
    session.invalidate()
    Found("/")
  }

  get("/@:accountName") {
    val accountName = params("accountName")
    DB readOnly { implicit dbsession =>
      import User.u, Post.p

      sql"SELECT ${u.resultAll} FROM ${User as u} WHERE ${u.accountName} = ${accountName} AND ${u.delFlg} = 0"
        .map(User(_))
        .first()
        .apply() match {
        case None =>
          NotFound()
        case Some(user) =>
          val userId = user.id
          val posts =
            sql"SELECT ${p.result.id}, ${p.result.userId}, ${p.result.body}, ${p.result.createdAt}, ${p.result.mime} FROM ${Post as p} WHERE ${p.userId} = ${userId} ORDER BY ${p.createdAt} DESC"
              .map(Post.withoutImage)
              .list()
              .apply()
          val postResults = makePostResults(posts)

          val commentCount =
            sql"SELECT COUNT(*) AS count FROM `comments` WHERE `user_id` = ${userId}"
              .map(_.int(1))
              .single()
              .apply()
              .getOrElse(0)

          val postIds =
            sql"SELECT `id` FROM `posts` WHERE `user_id` = ${userId}"
              .map(_.int(1))
              .list()
              .apply()

          val postCount = postIds.size

          val commentedCount = if (postCount > 0) {
            sql"SELECT COUNT(*) AS count FROM `comments` WHERE `post_id` IN (${postIds})"
              .map(_.int(1))
              .first()
              .apply()
              .getOrElse(0)
          } else {
            0
          }

          implicit val xkey: String @@ XSRFKey = Tag[XSRFKey](xsrfKey)
          implicit val xtoken: String @@ XSRFToken = Tag[XSRFToken](xsrfToken)
          Ok(
            views.html.user(
              getSessionUser,
              user,
              postResults,
              postCount,
              commentCount,
              commentedCount
            )
          )
      }
    }
  }

  get("/posts") {
    import com.github.nscala_time.time.Imports._
    import Post.p

    val maxCreatedAt = params.get("max_created_at") map { param: String =>
        DateTime.parse(param)
    }

    val posts = DB readOnly { implicit session =>
      sql"SELECT ${p.result.id}, ${p.result.userId}, ${p.result.body}, ${p.result.createdAt}, ${p.result.mime} FROM ${Post as p} WHERE ${p.createdAt} <= ${maxCreatedAt.getOrElse(null)} ORDER BY ${p.createdAt} DESC"
        .map(Post.withoutImage)
        .list()
        .apply()
    }

    implicit val xkey: String @@ XSRFKey = Tag[XSRFKey](xsrfKey)
    implicit val xtoken: String @@ XSRFToken = Tag[XSRFToken](xsrfToken)
    Ok(views.html.posts(makePostResults(posts)))
  }
}
