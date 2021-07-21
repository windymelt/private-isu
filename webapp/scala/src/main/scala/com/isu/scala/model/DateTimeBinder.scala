package com.isu.scala.model

import scalikejdbc._
import com.github.nscala_time.time.Imports._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaBinders._

object DateTimeBinder {
  implicit val dateTimeTypeBinder: TypeBinder[DateTime] =
    new TypeBinder[DateTime] {
      def apply(rs: java.sql.ResultSet, label: String): DateTime = new DateTime(
        rs.getTimestamp(label).getTime()
      )
      def apply(rs: java.sql.ResultSet, index: Int): DateTime = new DateTime(
        rs.getTimestamp(index).getTime()
      )
    }
}
