package org.musicpimp

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.support.multidex.MultiDexApplication
import com.mle.android.exceptions.ExplainedException
import com.mle.util.Version
import org.musicpimp.andro.messaging.IMessagingUtils
import org.musicpimp.http.Endpoint
import org.musicpimp.iap.amazon.AmazonIAPActivity
import org.musicpimp.iap.google.GooglePlayIAPActivity
import org.musicpimp.iap.samsung.SamsungIapActivity
import org.musicpimp.iap.{AmazonPimpIapUtils, GooglePimpIapUtils, PimpIapUtils, SamsungPimpIapUtils}
import org.musicpimp.messaging.{PimpAmazonMessaging, PimpGoogleMessaging}

class PimpApp extends MultiDexApplication {
  override def onCreate(): Unit = {
    super.onCreate()
    PimpApp.ctx = Some(this)
  }
}

object PimpApp {
  // ctx.foreach(_.getResources.getString(R.string.app_version))
  // TODO inflate the version string from strings.xml
  val version = Version(BuildInfo.version)
  // class can access private members of companion object, yo
  private var ctx: Option[Context] = None

  def context = ctx.getOrElse(throw new ExplainedException("Context is needed but not available."))

  lazy val appStoreInfo = AppStoreInfo.fromName(BuildInfo.appStore)

  object AppStores extends Enumeration {
    type AppStore = Value
    val GooglePlay, Amazon, Samsung, None = Value
  }

  abstract class AppStoreInfo(val appStore: AppStores.Value,
                              val iapUtils: PimpIapUtils,
                              val iapActivity: Class[_ <: Activity],
                              val pushUtils: Option[Endpoint => IMessagingUtils]) {
    protected def marketUriString(packageName: String): String

    def marketUri(packageName: String): Uri = Uri parse marketUriString(packageName)

    def supportsMessaging = pushUtils.isDefined
  }

  object AppStoreInfo {
    def fromName(name: String): AppStoreInfo = AppStores.withName(name) match {
      case AppStores.GooglePlay => GooglePlayInfo
      case AppStores.Amazon => AmazonAppStoreInfo
      case AppStores.Samsung => SamsungAppStoreInfo
      case AppStores.None => GooglePlayInfo
    }
  }

  object GooglePlayInfo extends AppStoreInfo(
    AppStores.GooglePlay,
    GooglePimpIapUtils,
    classOf[GooglePlayIAPActivity],
    Some(e => new PimpGoogleMessaging(e))) {
    protected def marketUriString(packageName: String) = s"market://details?id=$packageName"
  }

  object AmazonAppStoreInfo extends AppStoreInfo(
    AppStores.Amazon,
    AmazonPimpIapUtils,
    classOf[AmazonIAPActivity],
    Some(e => new PimpAmazonMessaging(e))) {
    protected def marketUriString(packageName: String) = s"amzn://apps/android?p=$packageName"
  }

  object SamsungAppStoreInfo extends AppStoreInfo(
    AppStores.Samsung,
    SamsungPimpIapUtils,
    classOf[SamsungIapActivity],
    None) {
    protected def marketUriString(packageName: String): String = s"samsungapps://ProductDetail/$packageName"
  }

}
