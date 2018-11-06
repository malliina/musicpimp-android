import android.Keys._
import android.AndroidApp
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._

object AndroidBuild extends Build {
  val usedScalaVersion = "2.11.12"

  val localMavenDir = new File(sys.env("ANDROID_HOME") + "/extras/android/m2repository")

  object AppStores extends Enumeration {
    type AppStore = Value
    val GooglePlay, Amazon, Samsung, None = Value
  }

  /**
   * Determines the correct app store implementation when building the app.
   */
  val appStore = settingKey[AppStores.Value]("The appstore for which the app is built")
  val storeFileExtension = settingKey[String]("File suffix depending on store")
  val apkFileName = settingKey[String]("Name of .apk, without the .apk extension")
  val packageStore = taskKey[File]("Builds a package ready to be uploaded to an app store")

//  lazy val root = Project(id = "pimp", base = file(".")) settings (Seq(
//    scalaVersion := usedScalaVersion,
//    packageT in Compile <<= packageT in Android in app,
//    packageStore <<= packageRelease in Android in app,
//    packageRelease <<= packageRelease in Android in app,
//    packageDebug <<= packageDebug in Android in app,
//    install <<= install in Android in app,
//    run <<= run in Android in app,
//    commands <<= commands in app
//  ) ++ android.Plugin.androidCommands: _*
//    ) aggregate app

  lazy val app = Project("musicpimp", file("musicpimp"))
    .settings(pimpSettings: _*)
    .enablePlugins(AndroidApp)

  val mleGroup = "com.github.malliina"
  val supportGroup = "com.android.support"
  val supportVersion = "19.1.0"

  def apkSettings = Seq(
    appStore := AppStores.GooglePlay,
    storeFileExtension := {
      appStore.value match {
        case AppStores.GooglePlay => "-google"
        case AppStores.Amazon => "-amazon"
        case AppStores.Samsung => "-samsung"
        case AppStores.None => "-none"
      }
    },
    apkFileName := s"${name.value}${storeFileExtension.value}-${version.value}",
    packageStore := {
      val origFile: File = (packageRelease in Android).value
      val newFile = fileNamed(apkFileName.value, origFile)
      val (destFile, logMsg) = moveFileWithOverwrite(origFile, newFile)
      streams.value.log info logMsg
      destFile
    }
  )

//  def cache(org: String, module: String)(packagePrefix: String, more: String*): ProguardCache =
//    cache(org)(packagePrefix, more: _*) % module
//
//  def cache(org: String)(packagePrefix: String, more: String*): ProguardCache =
//    cacheSeq(org)(Seq(packagePrefix) ++ more)
//
//  def cacheSeq(org: String)(packages: Seq[String]) =
//    ProguardCache(packages: _*) % org

  lazy val pimpSettings = apkSettings ++ commonSettings ++
    googlePlayServicesSettings ++ amazonDeviceMessagingSettings ++ rxSettings ++ Seq(
    scalaVersion := usedScalaVersion,
    version := "2.0.2",
    resolvers ++= Seq(
      "typesafe" at "http://repo.typesafe.com/typesafe/maven-releases/",
      "google" at "https://maven.google.com/"
    ),
    libraryDependencies ++= Seq(
      aar(supportGroup % "appcompat-v7" % supportVersion),
      zxingDep,
      aar(mleGroup %% "util-android" % "0.9.8"),
      "com.google.android.gms" % "play-services" % "4.4.52",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    ),
    typedResourcesAar := true,
    typedViewHolders := true,
    useProguard in Android := true,
    proguardCache in Android ++= Seq(
      "com.google",
      "org.joda",
      "joda-time",
      "io.reactivex",
      "com.fasterxml.jackson",
      "com.typesafe.play",
      "com.google.zxing",
      "android.support.v4",
      "android.support.v7",
      "com.loopj.android"
//      cache(org = supportGroup, module = "appcompat-v7")(packagePrefix = "android.support.v7"),
//      cache(supportGroup, "support-v4")("android.support.v4"),
//      cacheSeq("com.google.zxing")(cachedZxingPackages),
//      cache(mleGroup)("com.mle"),
//      cache("com.loopj.android")("com.loopj.android"),
//      cache("org.java-websocket")("org.java_websocket"),
//      cache("com.typesafe.play")("play"),
//      cache("joda-time")("org.joda"),
//      cache("org.joda")("org.joda"),
//      cache("com.fasterxml.jackson.core")("com.fasterxml.jackson"),
//      cache("com.google.android.gms")("com.google.android.gms", "com.google.ads")
    ),
    proguardOptions in Android ++= Seq(
      // I think there's a problem because play-json depends on org.w3c.something, which is
      // already included in Android by default, maybe I could try excluding org.w3c.* from
      // my library dependency declarations instead
      "-dontwarn " + (dontWarnClasses mkString ","),
      // maybe fixes processEncodedAnnotation bs
      "-keep class " + (keptClasses mkString ","),
      // both below are required by Amazon IAP
      "-keepattributes *Annotation*,Signature",
      "-dontoptimize",
      "-dontnote " + (dontNoteClasses mkString ",")
    ),
    packagingOptions in Android := PackagingOptions(excludes = Seq("LICENSE.txt", "NOTICE.txt", "LICENSE", "NOTICE").map(file => s"META-INF/$file")),
//    apkbuildExcludes in Android ++= Seq("LICENSE.txt", "NOTICE.txt", "LICENSE", "NOTICE").map(file => s"META-INF/$file"),
    localAars in Android ++= Seq("scanner", "android-utils", "samsung-iap-lib").map(name => baseDirectory.value / "aar" / s"$name.aar")
  ) ++ buildMetaSettings

  def rxSettings = {
    val rxGroup = "io.reactivex"
    Seq(
      libraryDependencies ++= Seq(
        rxGroup %% "rxscala" % "0.24.1",
        rxGroup % "rxandroid" % "0.25.0"
      ),
//      proguardCache in Android += cache(rxGroup)("rx"),
      proguardOptions in Android ++= Seq("-dontwarn sun.misc.Unsafe, rx.lang.scala.**")
    )
  }

  def googlePlayServicesSettings = Seq(
    resolvers += "Local google .aar maven repo" at localMavenDir.toURI.toString,
    libraryDependencies ++= Seq(
      supportGroup % "support-v4" % supportVersion
    ),
    proguardOptions in Android ++= Seq(
      """-keep class * extends java.util.ListResourceBundle {
            protected java.lang.Object[][] getContents();
        }""",
      """-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
            public static final *** NULL;
        }""",
      "-keepnames @com.google.android.gms.common.annotation.KeepName class *",
      """-keepclassmembernames class * {
            @com.google.android.gms.common.annotation.KeepName *;
        }""",
      """-keepnames class * implements android.os.Parcelable {
            public static final ** CREATOR;
        }""",
      "-dontnote com.google.android.gms.ads.**,com.google.android.gms.plus.PlusOneButton,com.google.android.gms.maps.internal.u,com.google.android.gms.internal.ku"
    )
  )

  /**
   * https://developer.amazon.com/public/apis/engage/device-messaging/tech-docs/03-setting-up-adm
   *
   * Assumes the ADM stub JAR is put into folder extlib/
   */
  def amazonDeviceMessagingSettings = Seq(
    // Adds the ADM stub jar to the classpath for compilation
    unmanagedClasspath in Compile <++= baseDirectory map { base => (base / "extlib" ** "*.jar").classpath},
    // Ensures the ADM stub jar is not packaged with the APK; the ADM classes are already available on Amazon devices
    dependencyClasspath in Android ~= {
      _ filterNot (_.data.getName startsWith "amazon-device-messaging")
    },
    proguardOptions in Android ++= Seq(
      // This should point to the directory where ADM’s jar is stored
      "-libraryjars musicpimp/extlib",
      "-dontwarn com.amazon.device.messaging.**",
      "-keep class com.amazon.device.messaging.** {*;}",
      "-keep public class * extends com.amazon.device.messaging.ADMMessageReceiver",
      "-keep public class * extends com.amazon.device.messaging.ADMMessageHandlerBase",
      "-dontnote com.amazon.**"
    )
  )

  def moveFileWithOverwrite(origFile: File, newFile: File): (File, String) = {
    if (newFile.exists()) {
      newFile.delete()
    }
    if (origFile renameTo newFile) {
      val newPath = newFile.getAbsolutePath
      (newFile, s"Moved to: $newPath")
    } else {
      (origFile, "Failed to move file.")
    }
  }

  def dontNoteClasses = Seq(
    "com.fasterxml.**",
    "org.joda.time.DateTimeZone,scala.reflect.**",
    "scala.concurrent.util.Unsafe,scala.concurrent.stm.impl.**",
    "scala.Enumeration$$anonfun$scala$Enumeration$$isValDef$1$1")
  
  def dontWarnClasses = Seq("org.w3c.**", "com.amazon.**", "org.apache.**", "org.joda.**", "scala.collection.**", "play.api.libs.**")

  def zxingDep = "com.google.zxing" % "core" % "2.3.0"

  def zxingPrefix = "com.google.zxing."

  def keptClasses: Seq[String] = keptZxingClasses ++ Seq(
    "com.fasterxml.**",
    "com.android.vending.billing.**",
    "com.amazon.** {*;}")

  def keptZxingClasses: Seq[String] = Seq(
    "client.android.**",
    "Result",
    "ResultPoint",
    "ResultCallback",
    "ResultPointCallback",
    "PlanarYUVLuminanceSource",
    "BarcodeFormat",
    "ResultMetadataType"
  ).map(suffix => zxingPrefix + suffix)

  /**
   * The Proguard-cached Zxing packages.
   *
   * All the zxing packages except com.google.zxing.client.android,
   * because that package is in the library project, while all others
   * are in the .jar and should be proguard cached.
   *
   * @see the above proguard rules
   * @return all the zxing packages to include in the proguard cache
   */
  def cachedZxingPackages: Seq[String] = Seq(
    "aztec", "common", "datamatrix",
    "maxicode", "multi", "oned",
    "pdf417", "qrcode", "client.result"
  ).map(name => zxingPrefix + name)

  lazy val commonSettings = Seq(
    scalaVersion := usedScalaVersion,
    resolvers ++= Seq(
      // assumes you have installed "android support repository" from SDK Manager first
      // "Local .aar maven repo" at new File(sys.env("ANDROID_HOME") + "/extras/android/m2repository").toURI.toString
      "Local .aar maven repo" at localMavenDir.toURI.toString
    ),
    platformTarget in Android := "android-19",
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6"
  )

  def buildMetaSettings = sbtbuildinfo.Plugin.buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "org.musicpimp",
    buildInfoKeys := Seq[BuildInfoKey](appStore, version)
  )

  /**
   * Injects `str` to the name of `origFile`, after the file name but before the file extension.
   *
   * @param str string to append
   * @param origFile the base file
   * @return a new file with the appended name, same parent as `origFile`
   */
  def appendString(str: String, origFile: File): File = {
    val (noext, ext) = splitFileName(origFile)
    val newFileName = s"$noext$str$ext"
    fileNamed(newFileName, origFile)
  }

  def splitFileName(file: File): (String, String) = {
    val fileName = file.getName
    val maybeIdx = fileName lastIndexOf "."
    if (maybeIdx >= 0) fileName splitAt maybeIdx else ("", "")
  }

  def extension(file: File) = splitFileName(file)._2

  def fileNamed(newName: String, origFile: File): File = {
    val nameWithExt = newName + extension(origFile)
    Option(origFile.getParentFile).fold(file(nameWithExt))(_ / nameWithExt)
  }
}