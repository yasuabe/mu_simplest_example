package mu_example.simplest.server

import cats.effect._
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import higherkindness.mu.rpc.config.server.BuildServerFromConfig
import monix.eval.{Task, TaskApp}
import mu_example.simplest.protocol.Protocols._

import scala.concurrent.ExecutionContext

class SimplestServiceHandler[F[_]: ConcurrentEffect] extends SimplestService[F] {
  def greet(name: Name): F[Greet] = Async[F].delay {
    println(s"Greeting to ${name.value} ...")
    Greet(s"Hello, ${name.value}!")
  }
}

object ServerApp extends TaskApp {
  implicit val alg: SimplestService[Task] = new SimplestServiceHandler[Task]
  implicit val ec:  ExecutionContext      = scheduler

  def run(args: List[String]): Task[ExitCode] = for {
    configs <- SimplestService.bindService[Task].map(AddService)
    server  <- BuildServerFromConfig[Task]("rpc.server.port", List(configs))
    _       <- Task(println(s"Server is starting ..."))
    _       <- GrpcServer.server[Task](server)
  } yield ExitCode.Success
}
