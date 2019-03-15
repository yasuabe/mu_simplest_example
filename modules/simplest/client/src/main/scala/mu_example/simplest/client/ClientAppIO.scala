package mu_example.simplest.client

import cats.effect._
import cats.{ApplicativeError, Functor}
import cats.effect.{Async, ConcurrentEffect, Resource}
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import monix.execution.Scheduler
import io.grpc.StatusRuntimeException
import mu_example.simplest.protocol.Protocols.{Name, SimplestService}
import higherkindness.mu.rpc.config.channel.ConfigForAddress

trait SimplestClient[F[_]] {
  def greet(name: String): F[Unit]
}
case class SimplestClientHandler[F[_]: ConcurrentEffect](
    client: Resource[F, SimplestService[F]]) extends SimplestClient[F] {
  def greet(name: String): F[Unit] =
    Async[F].delay(println(s"greeting to $name")) *>
      client.use(_.greet(Name(name)))
        .map { g => println(s"got a greet from server: $g") }
        .handleErrorWith {
          case e: StatusRuntimeException =>
            Async[F].delay(println(s"RPC failed: ${e.getStatus} $e")) *>
              ApplicativeError[F, Throwable].raiseError(e)
        }
}
object SimplestClientHandler {
  import Scheduler.Implicits.global
  def apply[F[_]: Functor: ConcurrentEffect]: F[SimplestClient[F]] =
    ConfigForAddress[F]("rpc.client.host", "rpc.client.port") map { ch =>
      SimplestClientHandler[F](SimplestService.client[F](ch))
    }
}
object ClientAppIO extends IOApp {
  def printIO(line: String): IO[Unit] = IO {
    println(s"${ Thread.currentThread().getId }: $line")
  }
  def run(args: List[String]): IO[ExitCode] = for {
    _ <- printIO(s"Starting client, interpreting to Future ...")
    c <- SimplestClientHandler[IO]
    _ <- c.greet("World")
    _ <- printIO(s"Finishing program interpretation ...")
  } yield ExitCode.Success
}
