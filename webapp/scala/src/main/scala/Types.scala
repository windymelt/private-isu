package com.isu.scala

object Types {
  type ID = BigInt
  sealed trait XSRFKey
  sealed trait XSRFToken

  // Tagged type
  type Tagged[U] = { type Tag = U }
  type @@[T, U] = T with Tagged[U]
  class Tagger[U] {
    def apply[T](t: T): T @@ U = t.asInstanceOf[T @@ U]
  }
  def Tag[U] = new Tagger[U]
}