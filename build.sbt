import AndroidBuild._

lazy val app = Project("musicpimp", file("musicpimp"))
  .settings(pimpSettings: _*)
  .enablePlugins(AndroidApp)

val mleGroup = "com.github.malliina"
val supportGroup = "com.android.support"
val supportVersion = "19.1.0"
val usedScalaVersion = "2.11.12"

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

lazy val commonSettings = Seq(
  scalaVersion := usedScalaVersion,
  resolvers ++= Seq(
    // assumes you have installed "android support repository" from SDK Manager first
    "Local .aar maven repo" at localMavenDir.toURI.toString
  ),
  platformTarget in Android := "android-19",
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  scalacOptions += "-target:jvm-1.6"
)

/**
  * https://developer.amazon.com/public/apis/engage/device-messaging/tech-docs/03-setting-up-adm
  *
  * Assumes the ADM stub JAR is put into folder extlib/
  */
def amazonDeviceMessagingSettings = Seq(
  // Adds the ADM stub jar to the classpath for compilation
  unmanagedClasspath in Compile ++= (baseDirectory.value / "extlib" ** "*.jar").classpath,
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

def buildMetaSettings = sbtbuildinfo.Plugin.buildInfoSettings ++ Seq(
  sourceGenerators in Compile += buildInfo,
  buildInfoPackage := "org.musicpimp",
  buildInfoKeys := Seq[BuildInfoKey](appStore, version)
)

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
