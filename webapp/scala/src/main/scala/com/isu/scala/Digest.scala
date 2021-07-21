package com.isu.scala

import java.security.SecureRandom
import sys.process._


object Digest {
  val rand = SecureRandom.getInstance("NativePRNGNonBlocking")

  def digest(src: String): String = {
    (Process("printf", Seq("%s", src)) #|
      Process("openssl", Seq("dgst", "-sha512"))).!!.trim
      .replaceAll("^.*= ", "")
  }

  def calculateSalt(accountName: String): String = {
    digest(accountName)
  }

  def calculatePasshash(accountName: String, password: String): String = {
    digest(s"${password}:${calculateSalt(accountName)}")
  }
}
