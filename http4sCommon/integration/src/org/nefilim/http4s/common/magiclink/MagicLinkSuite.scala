package org.nefilim.http4s.common.magiclink

import cats.effect.IO
import cats.effect.kernel.Resource
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import dev.profunktor.redis4cats.data
import munit.CatsEffectSuite
import org.http4s.{Request, Uri}
import org.http4s.implicits.http4sLiteralsSyntax
import org.nefilim.http4s.common.id.IDGenerator
import org.nefilim.http4s.common.tokenstore.redis.RedisTokenStore
import org.testcontainers.containers.wait.strategy.Wait
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class MagicLinkSuite extends CatsEffectSuite with TestContainerForAll {
  override val containerDef: GenericContainer.Def[GenericContainer] = GenericContainer.Def(
    "redis:6",
    exposedPorts = Seq(6379),
    waitStrategy = Wait.forListeningPort()
  )

  def redisConfig(container: GenericContainer) = RedisTokenStore.Config(
    uri = s"redis://${container.containerIpAddress}:${container.mappedPort(6379)}",
    keySplitEpi = RedisMagicLinkStore.magicLinkTokenIDSplitEpi,
    keyPrefix = Some("ml")
  )
  def tokenAuthURI(): Uri =
    Uri.fromString(s"/tokenauth?t=$generatedID").getOrElse(fail("bad URI"))

  val generatedID = "abcdef"

  test("issuing the token") {
    withContainers { container =>
      implicit val redisCodec: data.RedisCodec[MagicLinkService.MagicLinkTokenID, String] =
        RedisMagicLinkStore.magicLinkTokenCodec[String](redisConfig(container))

      (for {
        case implicit0(logger: Logger[IO]) <- Resource.eval(Slf4jLogger.create[IO])
        redisStore <- RedisMagicLinkStore[IO, String](redisConfig(container))
        service = MagicLinkService(redisStore, TestIDGenerator(generatedID))
      } yield {
        service.session(uri"/tokenauth", "123")
      }).allocated.flatMap { case (io, cancel) =>
        io.assertEquals(tokenAuthURI()) >> cancel
      }
    }
  }

  test("redeeming the token") {
    withContainers { container =>
      implicit val redisCodec: data.RedisCodec[MagicLinkService.MagicLinkTokenID, String] =
        RedisMagicLinkStore.magicLinkTokenCodec[String](redisConfig(container))

      (for {
        case implicit0(logger: Logger[IO]) <- Resource.eval(Slf4jLogger.create[IO])
        redisStore <- RedisMagicLinkStore[IO, String](redisConfig(container))
        service = MagicLinkService(redisStore, TestIDGenerator(generatedID))
      } yield {
        service.session(uri"/tokenauth", "123") >>
          service.redeem(Request[IO](uri = tokenAuthURI()))
      }).allocated.flatMap { case (io, cancel) =>
        io.assertEquals(Some("123")) >> cancel
      }
    }
  }
}

case class TestIDGenerator(t: String) extends IDGenerator[IO] {
  override def id(): IO[String] = IO.pure(t)
}
