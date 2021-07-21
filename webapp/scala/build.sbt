val ScalatraVersion = "2.7.1"

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / organization := "com.isu"

lazy val hello = (project in file("."))
  .settings(
    name := "scala",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "org.scalatra" %% "scalatra" % ScalatraVersion,
      "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
      "org.scalatra" %% "scalatra-forms" % ScalatraVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
      "org.eclipse.jetty" % "jetty-webapp" % "9.4.35.v20201120" % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
      "com.github.nscala-time" %% "nscala-time" % "2.28.0",
      // MySQLコネクタ．これがないとscalikeJDBCはMySQLに接続できない
      "mysql" % "mysql-connector-java" % "8.0.22",
      // ScalikeJDBC本体
      "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
      // テスト用にh2 DBを使えるようにしておく（この記事では使わない）
      "com.h2database" % "h2" % "1.4.200", // for test purpose
      // テスト時にいろいろ助けてくれるらしいパッケージ
      "org.scalikejdbc" %% "scalikejdbc-test" % "3.5.0" % "test",
      // 今回はapplication.confに接続・コネクションプールの設定を記述する．
      // それを読み取るためのパッケージ
      "org.scalikejdbc" %% "scalikejdbc-config" % "3.5.0",
      // autoConstruct macro
      "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % "3.5.0",
      "org.scalikejdbc" %% "scalikejdbc-joda-time" % "3.5.0" // use joda time / nscalatime
    )
  )

enablePlugins(SbtTwirl)
enablePlugins(JettyPlugin)

TwirlKeys.templateImports ++= Seq(
  "com.isu.scala.helpers._",
  "com.isu.scala.Types._",
  "com.isu.scala.model._"
)

// task
import scala.sys.process._ 

lazy val initializedb = taskKey[Unit]("Initialize DB")

initializedb := {
    "./initialize-docker-db.sh" !
}