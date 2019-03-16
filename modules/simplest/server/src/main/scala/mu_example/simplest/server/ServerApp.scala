package mu_example.simplest.server

import cats.effect._
import cats.syntax.apply._
import cats.syntax.applicative._
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import higherkindness.mu.rpc.config.server.BuildServerFromConfig
import monix.eval.{Task, TaskApp}
import mu_example.simplest.protocol.Protocols._
import scala.concurrent.ExecutionContext

import ServerApp.put

class SimplestServiceHandler[F[_]: ConcurrentEffect] extends SimplestService[F] {
  def greet(n: Name): F[Greeting] =
    put[F](s"Greeting to ${n.value} ...") *> Greeting(s"Hello, ${n.value}!").pure[F]
}

object ServerApp extends TaskApp {
  implicit val alg: SimplestService[Task] = new SimplestServiceHandler[Task]
  implicit val ec:  ExecutionContext      = scheduler

  def put[F[_]: Async](line: String): F[Unit] = Async[F].delay(println(line))

  def run(args: List[String]): Task[ExitCode] = for {
    configs <- SimplestService.bindService[Task].map(AddService)
    server  <- BuildServerFromConfig[Task]("rpc.server.port", List(configs))
    _       <- put[Task](s"Server is starting ...")
    _       <- GrpcServer.server[Task](server)
  } yield ExitCode.Success
}
