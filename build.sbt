name := "confession-clock"

version := "1.0"

scalaVersion := "2.11.8"

mainClass in assembly := Some("ConfessionClock")

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "latest.integration"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.12.0"
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
libraryDependencies += "org.scala-lang" % "scala-swing" % "2.11+"