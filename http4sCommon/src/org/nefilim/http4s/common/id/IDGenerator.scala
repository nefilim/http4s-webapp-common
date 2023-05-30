package org.nefilim.http4s.common.id

import cats.Monad
import cats.syntax.all.*
import cats.effect.std.SecureRandom

import java.util.Base64

trait IDGenerator[F[_]] {
  def id(): F[String]
}

object IDGenerator {
  def secureIDGenerator[F[_]: Monad](
    secureRandom: SecureRandom[F],
    lengthInBytes: Int = 32,
    encoder: Base64.Encoder = Base64.getUrlEncoder.withoutPadding(),
  ): IDGenerator[F] = () => {
    secureRandom.nextBytes(lengthInBytes).map(a => encoder.encodeToString(a))
  }
}
