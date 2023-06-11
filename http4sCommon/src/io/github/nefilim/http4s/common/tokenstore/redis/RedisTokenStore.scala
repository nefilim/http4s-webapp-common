package io.github.nefilim.http4s.common.tokenstore.redis

import cats.Monad
import cats.effect.{Async, Resource}
import cats.syntax.all.*
import ciris.{ConfigValue, env}
import dev.profunktor.redis4cats.codecs.Codecs
import dev.profunktor.redis4cats.codecs.splits.SplitEpi
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.github.nefilim.http4s.common.tokenstore.TokenStore
import io.lettuce.core.{ClientOptions, TimeoutOptions}
import io.github.nefilim.http4s.common.tokenstore.TokenStore
import io.github.nefilim.http4s.common.tokenstore.redis.RedisTokenStore.Config
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.DurationConverters.ScalaDurationOps

object RedisTokenStore {
  // TODO refactor this to a trait and add http4sCommon-ciris module once spinning out
  case class Config[K](
    uri: String,
    keySplitEpi: SplitEpi[String, K],
    keyPrefix: Option[String] = None,
    expirationDuration: Option[FiniteDuration] = None,
  )

  object Config {
    def load[K](keySplitEpi: SplitEpi[String, K]): ConfigValue[ciris.Effect, Config[K]] = {
      for {
        uri <- env("H4S_COMMON_STORE_REDIS_URI")
        prefix <- env("H4S_COMMON_STORE_KEY_PREFIX").option
        exp <- env("H4S_COMMON_STORE_EXPIRATION").as[FiniteDuration].option
      } yield Config(uri, keySplitEpi, prefix, exp)
    }
  }

  def apply[F[_]: Async : Logger, K, V](
    config: Config[K],
    clientOptions: ClientOptions = defaultClientOptions,
  )(implicit codec: RedisCodec[K, V]): Resource[F, TokenStore[F, K, V]] = {
    for {
      options <- Resource.eval(Async[F].delay(clientOptions)) // pure?
      redis <- Redis[F].withOptions[K, V](config.uri, options, codec)
    } yield new RedisTokenStore[F, K, V](config, redis)
  }

  val defaultClientOptions = ClientOptions.builder()
    .autoReconnect(true)
    .pingBeforeActivateConnection(true)
    .timeoutOptions(
      TimeoutOptions.builder()
        .fixedTimeout(3.seconds.toJava)
        .build()
    )
    .build()

  implicit def tokenValueSplitEpi[V](
    implicit decoder: Decoder[V], encoder: Encoder[V]
  ): SplitEpi[String, V] = SplitEpi({ s =>
    decode[V](s).getOrElse(throw new IllegalArgumentException(s"unable to decode ${s}")) // ugh what to do, no error support in SplitEpi API
  }, { sv =>
    encoder(sv).deepDropNullValues.noSpaces
  })

  implicit def redisTokenStoreCodec[K, V](
    config: Config[K]
  )(implicit decoder: Decoder[V], encoder: Encoder[V]): RedisCodec[K, V] = {
    val totalKeySplitEpi = {
      config.keyPrefix.fold(config.keySplitEpi) { (kp: String) =>
        config.keySplitEpi.copy(
          get = { (a: String) => config.keySplitEpi.get(a.stripPrefix(s"$kp#")) },
          reverseGet = { (b: K) => s"$kp#${config.keySplitEpi.reverseGet(b)}" }
        )
      }
    }
    Codecs.derive(RedisCodec.Utf8, totalKeySplitEpi, tokenValueSplitEpi)
  }
}

private class RedisTokenStore[F[_] : Monad, K, V](
  config: Config[K],
  redis: RedisCommands[F, K, V],
) extends TokenStore[F, K, V] {

  override def store(id: K, token: V): F[Unit] = {
    config.expirationDuration.fold(redis.set(id, token))(d => redis.setEx(id, token, d))
  }

  override def findAndRemove(id: K): F[Option[V]] = {
    for {
      r <- redis.get(id)
      // no GETDEL atomic operation support yet in redis4cats?
      _ <- redis.del(id)
    } yield r
  }
}
