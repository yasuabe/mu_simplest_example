package mu_example.simplest.client

import cats.effect._
import cats.ApplicativeError
import cats.effect.{Async, ConcurrentEffect, Resource}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import io.grpc.StatusRuntimeException
import mu_example.simplest.protocol.Protocols.{Name, SimplestService}
import higherkindness.mu.rpc.config.channel.ConfigForAddress
import monix.eval.{Task, TaskApp}
import scala.concurrent.ExecutionContext

import ClientApp.put

case class SimplestClient[F[_]: ConcurrentEffect](
    service: Resource[F, SimplestService[F]]) {

  def greet(n: String): F[Unit] = put[F](s"greeting to $n") *>
    service.use(_.greet(Name(n)))
      .flatMap { g => put(s"got a greeting from server: $g") }
      .handleErrorWith {
        case e: StatusRuntimeException =>
          put(s"RPC failed: ${e.getStatus} $e") *>
            ApplicativeError[F, Throwable].raiseError(e)
      }
}
object SimplestClient {
  def apply[F[_]: ConcurrentEffect](implicit ec: ExecutionContext): F[SimplestClient[F]] =
    ConfigForAddress[F]("rpc.client.host", "rpc.client.port") map { channel =>
      SimplestClient[F](SimplestService.client[F](channel))
    }
}
object ClientApp extends TaskApp {
  implicit val ec: ExecutionContext = scheduler

  def put[F[_]: Async](line: String): F[Unit] = Async[F].delay(println(line))

  def run(args: List[String]): Task[ExitCode] = for {
    _      <- put[Task](s"Starting client ...")
    client <- SimplestClient[Task]
    _      <- client.greet("World")
    _      <- put[Task](s"Finishing program ...")
  } yield ExitCode.Success
}
