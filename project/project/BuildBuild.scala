import sbt.Keys._
import sbt._

object BuildBuild extends Build {
  // "build.sbt" goes here
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.10.7",
    sbtVersion := "0.13.17",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    resolvers ++= Seq(
      sbtResolver("releases"),
      sbtResolver("snapshots"),
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"),
    incOptions := incOptions.value.withNameHashing(true)
  ) ++ sbtPlugins

  def sbtResolver(suffix: String) =
    Resolver.url(s"scalasbt $suffix", new URL(s"http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-$suffix"))(Resolver.ivyStylePatterns)

  def sbtPlugins = Seq(
    "org.scala-android" % "sbt-android" % "1.7.10",
//    "com.hanhuy.sbt" % "android-sdk-plugin" % "1.3.10",
//    "com.hanhuy.sbt" % "sbt-idea" % "1.7.0-SNAPSHOT",
    "com.timushev.sbt" % "sbt-updates" % "0.1.6",
    "com.eed3si9n" % "sbt-buildinfo" % "0.3.0",
    "net.virtual-void" % "sbt-dependency-graph" % "0.7.4"
  ) map addSbtPlugin

  lazy val root = Project("plugins", file("."))
}
