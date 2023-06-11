 package io.github.nefilim.http4s.common.magiclink

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import ciris.{ConfigValue, env}
import dev.profunktor.redis4cats.codecs.splits.SplitEpi
import dev.profunktor.redis4cats.data.RedisCodec
import io.circe.{Decoder, Encoder}
import io.github.nefilim.http4s.common.id.IDGenerator
import io.github.nefilim.http4s.common.magiclink.MagicLinkService.MagicLinkTokenID
import io.github.nefilim.http4s.common.tokenstore.TokenStore
import io.github.nefilim.http4s.common.tokenstore.redis.RedisTokenStore
import io.github.nefilim.http4s.common.tokenstore.redis.RedisTokenStore.Config
import io.lettuce.core.ClientOptions
import org.http4s.{Request, Uri}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait MagicLinkService[F[_], T] {
  def session(redirectURI: Uri, token: T): F[Uri]
  def redeem(request: Request[F]): F[Option[T]]
}

object MagicLinkService {
  case class MagicLinkTokenID(id: String) extends AnyVal

  def apply[F[_]: Sync, T](
    store: MagicLinkStore[F, T],
    idGenerator: IDGenerator[F],
    tokenParameterLabel: String = TokenParameterLabel,
  ): MagicLinkService[F, T] = new MagicLinkService[F, T] {
    override def session(redirectURI: Uri, token: T): F[Uri] = {
      for {
        id <- idGenerator.id().map(MagicLinkTokenID)
        r = redirectURI.copy(query = redirectURI.query :+ tokenParameterLabel -> Some(id.id))
        _ <- store.store(id, token)
      } yield r
    }

    override def redeem(request: Request[F]): F[Option[T]] = {
      (for {
        tokenID <- OptionT.fromOption(request.params.get(tokenParameterLabel).map(MagicLinkTokenID))
        t <- OptionT(store.findAndRemove(tokenID))
      } yield t).value
    }
  }

  val TokenParameterLabel = "t"
}

trait MagicLinkStore[F[_], T] extends TokenStore[F, MagicLinkTokenID, T]

object RedisMagicLinkStore {
  def apply[F[_]: Async : Logger, T](
                                      config: Config[MagicLinkTokenID],
                                      clientOptions: ClientOptions = RedisTokenStore.defaultClientOptions,
  )(implicit codec: RedisCodec[MagicLinkTokenID, T]): Resource[F, MagicLinkStore[F, T]] = {
    RedisTokenStore[F, MagicLinkTokenID, T](config, clientOptions).map { s =>
      new MagicLinkStore[F, T] {
        override def store(id: MagicLinkTokenID, token: T): F[Unit] = s.store(id, token)
        override def findAndRemove(id: MagicLinkTokenID): F[Option[T]] = s.findAndRemove(id)
      }
    }
  }

  val magicLinkTokenIDSplitEpi: SplitEpi[String, MagicLinkTokenID] =
    SplitEpi(s => MagicLinkTokenID(s), sid => s"${sid.id}")

  implicit def magicLinkTokenCodec[V](
    config: RedisTokenStore.Config[MagicLinkTokenID]
  )(implicit decoder: Decoder[V], encoder: Encoder[V]): RedisCodec[MagicLinkTokenID, V] =
    RedisTokenStore.redisTokenStoreCodec(config)

  object Config {
    def load(): ConfigValue[ciris.Effect, RedisTokenStore.Config[MagicLinkTokenID]] = {
      for {
        uri <- env("C4S_MAGIC_LINK_REDIS_URI")
        prefix <- env("C4S_MAGIC_LINK_KEY_PREFIX").default("ml")
        exp <- env("C4S_MAGIC_LINK_EXPIRATION").as[FiniteDuration].default(5.minutes)
      } yield RedisTokenStore.Config(uri, magicLinkTokenIDSplitEpi, Some(prefix), Some(exp))
    }
  }
}