package io.github.nefilim.http4s.common.id

import cats.effect.IO
import cats.effect.std.SecureRandom
import munit.CatsEffectSuite
import cats.syntax.all.*

class IDGeneratorSuite extends CatsEffectSuite {
  def generator(bytes: Int): IO[IDGenerator[IO]] = for {
    sr <- SecureRandom.javaSecuritySecureRandom[IO]
    gen = IDGenerator.secureIDGenerator[IO](sr, bytes)
  } yield {
    gen
  }

  test("length should be always be longer than bytes") {
    (1 to 64).map { bytes =>
      val gen = generator(bytes)
      gen.flatMap(g => g.id()).map(id => id.length > bytes)
    }.toList.sequence.map(_.forall(identity)).assert
  }

  test("should never contain padding") {
    (1 to 64).map { bytes =>
      val gen = generator(bytes)
      gen.flatMap(g => g.id()).map(id => !id.contains("="))
    }.toList.sequence.map(_.forall(identity)).assert
  }

  test("sequential IDs should be unique") {
    val bytes = 8
    val gen = generator(bytes)
    gen.flatMap(g => List(g.id(), g.id(), g.id()).sequence).map(ids => ids.toSet.size).assertEquals(3)
  }
}
