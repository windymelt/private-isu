package com.isu.scala

object Config {
  import com.typesafe.config.ConfigFactory

  val conf: com.typesafe.config.Config = ConfigFactory.load();
}
