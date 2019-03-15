package mu_example.simplest.client

import cats.effect._
import cats.ApplicativeError
import cats.effect.{Async, ConcurrentEffect, Resource}
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import io.grpc.StatusRuntimeException
import mu_example.simplest.protocol.Protocols.{Name, SimplestService}
import higherkindness.mu.rpc.config.channel.ConfigForAddress
import monix.eval.{Task, TaskApp}
import scala.concurrent.ExecutionContext

case class SimplestClient[F[_]: ConcurrentEffect](
    client: Resource[F, SimplestService[F]]) {

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
object SimplestClient {
  def apply[F[_]: ConcurrentEffect](implicit ec: ExecutionContext): F[SimplestClient[F]] =
    ConfigForAddress[F]("rpc.client.host", "rpc.client.port") map { channel =>
      SimplestClient[F](SimplestService.client[F](channel))
    }
}
object ClientApp extends TaskApp {
  implicit val ec: ExecutionContext = scheduler

  private def printIO(line: String) = Task {
    println(s"${ Thread.currentThread().getId }: $line")
  }
  def run(args: List[String]): Task[ExitCode] = for {
    _      <- printIO(s"Starting client, interpreting to Future ...")
    client <- SimplestClient[Task]
    _      <- client.greet("World")
    _      <- printIO(s"Finishing program interpretation ...")
  } yield ExitCode.Success
}
