import AndroidBuild._

lazy val app = Project("musicpimp", file("musicpimp"))
  .settings(pimpSettings: _*)
  .enablePlugins(AndroidApp)

lazy val utils = Project("android-utils", file("android-utils"))
  .settings(utilsSettings: _*)
  .enablePlugins(AndroidLib)

run := run in Android in app

val malliinaGroup = "com.malliina"
val supportGroup = "com.android.support"
val supportVersion = "23.0.0"
val usedScalaVersion = "2.11.12"
val utilAndroidDep = malliinaGroup %% "util-android" % "0.12.5"


lazy val pimpSettings = apkSettings ++ okhttpSettings ++ commonSettings ++
  googlePlayServicesSettings ++ amazonDeviceMessagingSettings ++ rxSettings ++ Seq(
  scalaVersion := usedScalaVersion,
  version := "2.2.0",
  resolvers ++= Seq(
    "Typesafe" at "http://repo.typesafe.com/typesafe/maven-releases/",
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "Google" at "https://maven.google.com/",
    Resolver.bintrayRepo("malliina", "maven")
  ),
  libraryDependencies ++= Seq(
    aar(supportGroup % "appcompat-v7" % supportVersion),
    zxingDep,
    aar(utilAndroidDep),
    "com.typesafe.play" %% "play-json" % "2.3.10",
    "com.malliina" %% "okclient" % "1.7.1",
    "com.google.android.gms" % "play-services" % "8.4.0",
    "com.android.support" % "multidex" % "1.0.3",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  ),
  typedResourcesAar := true,
  typedViewHolders := true,
  dexMulti in Android := true,
  dexMainClassesConfig := baseDirectory.value / "maindexlist.txt",
  dexMinimizeMain in Android := true,
  // https://issuetracker.google.com/issues/37008143
  dexAdditionalParams in Android ++= Seq("--multi-dex", "--set-max-idx-number=40000"),
  //  shrinkResources in Android := true,
  useProguard in Android := true,
  proguardCache in Android ++= Seq(
    "android.support.v4",
    "android.support.v7",
    "com.android",
    "com.fasterxml.jackson",
    "com.google",
    "com.google.android",
    "com.google.zxing",
    "com.loopj.android",
    "com.typesafe.play",
    "cz.msebera.android",
    "io.reactivex",
    "joda-time",
    "org.joda"
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

def utilsSettings = commonSettings ++ Seq(
  organization := "com.malliina",
  version := "0.1.1",
  fork in Test := true,
  libraryDependencies ++= Seq(utilAndroidDep),
  platformTarget in Android := "android-27",
  useProguard in Android := true,
  libraryProject := true,
  typedResourcesAar := true
)

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
  platformTarget in Android := "android-27",
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  scalacOptions += "-target:jvm-1.6"
)

def okhttpSettings = Seq(
  proguardOptions in Android ++= Seq(
    "-dontwarn javax.annotation.**, okio.**",
    "-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase",
    "-dontwarn org.codehaus.mojo.animal_sniffer.*",
    "-dontwarn okhttp3.internal.platform.ConscryptPlatform"
  )
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
      rxGroup %% "rxscala" % "0.26.5",
      rxGroup % "rxandroid" % "0.25.0"
    ),
    //      proguardCache in Android += cache(rxGroup)("rx"),
    proguardOptions in Android ++= Seq("-dontwarn sun.misc.Unsafe, rx.lang.scala.**")
  )
}
