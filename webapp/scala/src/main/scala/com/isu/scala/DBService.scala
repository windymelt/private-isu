package com.isu.scala

trait DBService extends Loggable {
  import scalikejdbc._

// Setup connection-pool regards to application.conf.
// cf. application.conf.
  logger.info("initializing connection pool")
  scalikejdbc.config.DBs.setupAll()

// test
  val value = DB readOnly { implicit session =>
    sql"select 1 as one".map(_.long(1)).list.apply()
  }
  logger.info(s"DB Connection has been established: $value")
}
