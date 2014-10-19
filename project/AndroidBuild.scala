import android.Keys._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._

object AndroidBuild extends Build {

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

  lazy val root = Project(id = "pimp", base = file(".")) settings (Seq(
    scalaVersion := "2.11.2",
    packageT in Compile <<= packageT in Android in app,
    packageStore <<= packageRelease in Android in app,
    packageRelease <<= packageRelease in Android in app,
    packageDebug <<= packageDebug in Android in app,
    install <<= install in Android in app,
    run <<= run in Android in app,
    commands <<= commands in app
  ) ++ android.Plugin.androidCommands: _*
    ) aggregate app
//  ) aggregate(app, gcmLibProject)

  lazy val app = Project("musicpimp", file("musicpimp")).settings(pimpSettings: _*)
//  lazy val gcmLibProject = Project("gcm_lib", file("google-play-services_lib")).settings(libraryProjectSettings: _*)

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

  def cache(org: String, module: String)(packagePrefix: String, more: String*): ProguardCache =
    cache(org)(packagePrefix, more: _*) % module

  def cache(org: String)(packagePrefix: String, more: String*): ProguardCache =
    cacheSeq(org)(Seq(packagePrefix) ++ more)

  def cacheSeq(org: String)(packages: Seq[String]) =
    ProguardCache(packages: _*) % org

  // I think androidBuild(scannerLib) means that scannerLib is a compile-time dependency
  //  lazy val pimpSettings = android.Plugin.androidBuild(gcmLibProject) ++ apkSettings ++ commonSettings ++
  lazy val pimpSettings = android.Plugin.androidBuild ++ apkSettings ++ commonSettings ++
    googlePlayServicesSettings ++ amazonDeviceMessagingSettings ++ rxSettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++ Seq(
    scalaVersion := "2.11.2",
    version := "1.9.9",
    libraryDependencies ++= Seq(
      aar(supportGroup % "appcompat-v7" % supportVersion),
      zxingDep,
      mleGroup %% "util-android" % "0.9.1",
      mleGroup %% "util-base" % "0.2.0",
      "com.google.android.gms" % "play-services" % "4.4.52"
    ),
    useProguard in Android := true,
    proguardCache in Android ++= Seq(
      cache(org = supportGroup, module = "appcompat-v7")(packagePrefix = "android.support.v7"),
      cache(supportGroup, "support-v4")("android.support.v4"),
      cacheSeq("com.google.zxing")(cachedZxingPackages),
      cache(mleGroup)("com.mle"),
      cache("com.loopj.android")("com.loopj.android"),
      cache("org.java-websocket")("org.java_websocket"),
      cache("com.typesafe.play")("play"),
      cache("joda-time")("org.joda"),
      cache("org.joda")("org.joda"),
      cache("com.fasterxml.jackson.core")("com.fasterxml.jackson"),
      cache("com.google.android.gms")("com.google.android.gms", "com.google.ads")
    ),
    proguardOptions in Android ++= Seq(
      // I think there's a problem because play-json depends on org.w3c.something, which is
      // already included in Android by default, maybe I could try excluding org.w3c.* from
      // my library dependency declarations instead
      "-dontwarn " + (dontWarnClasses mkString ","),
      // maybe fixes processEncodedAnnotation bs
      "-keep class " + (keptClasses mkString ","),
      // both below are required by Amazon IAP
      "-keepattributes *Annotation*",
      "-dontoptimize"
    ),
    apkbuildExcludes in Android ++= Seq("LICENSE.txt", "NOTICE.txt", "LICENSE", "NOTICE").map(file => s"META-INF/$file"),
    localAars in Android ++= Seq("scanner", "android-utils", "samsung-iap-lib").map(name => baseDirectory.value / "aar" / s"$name.aar")
    // did not compile when I added gcm_lib as an .aar
    //    localProjects in Android <+= baseDirectory(b => AutoLibraryProject(b / ".." / "google-play-services_lib"))
  ) ++ buildMetaSettings

  def rxSettings = {
    val rxGroup = "com.netflix.rxjava"
    val rxVersion = "0.19.6"
    Seq(
      libraryDependencies ++= Seq(
        rxGroup % "rxjava-core" % rxVersion,
        rxGroup % "rxjava-scala" % rxVersion,
        rxGroup % "rxjava-android" % rxVersion
      ),
      proguardCache in Android += cache(rxGroup)("rx"),
      proguardOptions in Android ++= Seq("-dontwarn sun.misc.Unsafe")
    )
  }

  def googlePlayServicesSettings = Seq(
    resolvers += "Local google .aar maven repo" at new File(sys.env("ANDROID_HOME") + "/extras/google/m2repository").toURI.toString,
    libraryDependencies ++= Seq(
      supportGroup % "support-v4" % supportVersion
    ),
    proguardOptions in Android ++= Seq(
      """-keep class * extends java.util.ListResourceBundle {
            protected Object[][] getContents();
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
        }"""
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
      "-keep public class * extends com.amazon.device.messaging.ADMMessageHandlerBase"
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

  def dontWarnClasses = Seq("org.w3c.**", "com.amazon.**", "org.apache.**", "org.joda.**")

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
    scalaVersion := "2.11.2",
    resolvers ++= Seq(
      // assumes you have installed "android support repository" from SDK Manager first
      "Local .aar maven repo" at new File(sys.env("ANDROID_HOME") + "/extras/android/m2repository").toURI.toString
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

  lazy val libraryProjectSettings = android.Plugin.androidBuild ++ commonSettings ++ Seq(
    libraryProject := true
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