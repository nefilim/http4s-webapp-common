package io.github.nefilim.http4s.common.tokenstore

trait TokenStore[F[_], K, T] {
  def store(id: K, token: T): F[Unit]
  def findAndRemove(id: K): F[Option[T]]
}
