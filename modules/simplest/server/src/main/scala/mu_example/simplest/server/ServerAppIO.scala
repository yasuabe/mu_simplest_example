package mu_example.simplest.server

import cats.effect._
import monix.execution.Scheduler
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import higherkindness.mu.rpc.config.server.BuildServerFromConfig
import monix.eval.Task
import mu_example.simplest.protocol.Protocols._

class SimplestServiceHandler[F[_]: ConcurrentEffect](implicit E: Effect[Task])
  extends SimplestService[F] {

  override def greet(name: Name): F[Greet] = Async[F].delay {
    println(s"Greeting to ${name.value} ...")
    Greet(s"Hello, ${name.value}!")
  }
}
object ServerAppIO extends IOApp {
  implicit val S: Scheduler = Scheduler.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(S)
  implicit val algebra: SimplestService[IO] =
    new SimplestServiceHandler[IO]

  override def run(args: List[String]): IO[ExitCode] = for {
    configs <- SimplestService.bindService[IO].map(AddService)
    server  <- BuildServerFromConfig[IO]("rpc.server.port", List(configs))
    _       <- IO(println(s"Server is starting ..."))
    _       <- GrpcServer.server[IO](server)
  } yield ExitCode.Success
}
