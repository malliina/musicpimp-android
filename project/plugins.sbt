scalaVersion := "2.10.7"

sbtVersion := "0.13.17"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  ivyResolver("releases"),
  ivyResolver("snapshots"),
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

incOptions := incOptions.value.withNameHashing(true)

Seq(
  "org.scala-android" % "sbt-android" % "1.7.10",
  "com.eed3si9n" % "sbt-buildinfo" % "0.3.0"
) map addSbtPlugin

def ivyResolver(suffix: String) =
  Resolver.url(s"scalasbt $suffix", new URL(s"http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-$suffix"))(Resolver.ivyStylePatterns)
