package com.isu.scala

import org.scalatra._

trait CSRF extends XsrfTokenSupport {
  this: ScalatraBase =>
  override def xsrfKey = "csrf_token"
}
