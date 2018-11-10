import sbt._

object AndroidBuild {
  def localMavenDir: File = sys.env.get("ANDROID_HOME")
    .map(d => new File(s"$d/extras/android/m2repository"))
    .getOrElse(IO.temporaryDirectory)

  object AppStores extends Enumeration {
    type AppStore = Value
    val GooglePlay, Amazon, Samsung, None = Value
  }

  /** Determines the correct app store implementation when building the app.
    */
  val appStore = settingKey[AppStores.Value]("The appstore for which the app is built")
  val storeFileExtension = settingKey[String]("File suffix depending on store")
  val apkFileName = settingKey[String]("Name of .apk, without the .apk extension")
  val packageStore = taskKey[File]("Builds a package ready to be uploaded to an app store")

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
    "scala.Enumeration$$anonfun$scala$Enumeration$$isValDef$1$1",
    "cz.msebera.android.httpclient.extras.PRNGFixes",
    "com.google.android.gms.**"
    //    "com.google.android.gms.internal.zzlh",
    //    "com.google.android.gms.internal.zzlh$1",
    //    "com.google.android.gms.maps.internal.zzad"
  )

  def dontWarnClasses = Seq(
    "org.w3c.**",
    "com.amazon.**",
    "org.apache.**",
    "org.joda.**",
    "scala.collection.**",
    "play.api.libs.**",
    "cz.msebera.android.httpclient.extras.**",
    "com.google.android.gms.internal.**"
  )

  def zxingDep = "com.google.zxing" % "core" % "2.3.0"

  def zxingPrefix = "com.google.zxing."

  def keptClasses: Seq[String] = keptZxingClasses ++ Seq(
    "com.fasterxml.**",
    "com.android.vending.billing.**",
    "com.amazon.** {*;}"
  )

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

  /** The Proguard-cached Zxing packages.
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

  /** Injects `str` to the name of `origFile`, after the file name but before the file extension.
    *
    * @param str      string to append
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
