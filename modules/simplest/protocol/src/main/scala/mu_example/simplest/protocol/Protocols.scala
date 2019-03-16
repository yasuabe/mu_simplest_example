package mu_example.simplest.protocol

import higherkindness.mu.rpc.protocol._

object Protocols {
  @message
  case class Name(value: String)

  @message
  case class Greeting(value: String)

  @service(Protobuf)
  trait SimplestService[F[_]] {
    def greet(name: Name): F[Greeting]
  }
}
