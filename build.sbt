name := "mu-simplest-example"

version := "0.1"

scalaVersion := "2.12.8"

lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin("org.spire-math"  %% "kind-projector" % "0.9.4"),
  addCompilerPlugin("org.scalamacros" %% "paradise"       % "2.1.1" cross CrossVersion.full),
  scalacOptions += "-language:higherKinds",
  libraryDependencies ++= Seq(
    "io.higherkindness" %% "mu-rpc-monix" % "0.17.2",
    "io.higherkindness" %% "mu-config"    % "0.17.2"
  ),
)
lazy val protocol = project.in(file("modules/simplest/protocol"))
  .settings(commonSettings)

lazy val server = project.in(file("modules/simplest/server"))
  .dependsOn(protocol)
  .settings(commonSettings :+ (libraryDependencies += "io.higherkindness" %% "mu-rpc-server" % "0.17.2"))

lazy val client = project.in(file("modules/simplest/client"))
  .dependsOn(protocol)
  .settings(commonSettings :+ (libraryDependencies += "io.higherkindness" %% "mu-rpc-channel" % "0.17.2"))

lazy val allModules: Seq[ProjectReference] = Seq(
  protocol,
  server,
  client
)