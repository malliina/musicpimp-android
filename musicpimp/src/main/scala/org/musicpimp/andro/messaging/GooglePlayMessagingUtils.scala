package org.musicpimp.andro.messaging

import android.app.Activity
import android.content.{SharedPreferences, Context}
import android.preference.PreferenceManager
import com.google.android.gms.common.{ConnectionResult, GooglePlayServicesUtil}
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.mle.android.util.PreferenceImplicits._
import com.mle.android.util.UtilLog
import com.mle.concurrent.ExecutionContexts.cached
import scala.concurrent.Future

/**
  */
trait GooglePlayMessagingUtils extends IMessagingUtils with UtilLog {
  def serverMessenger: JsonMessagingUtils

  private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
  private val REG_PREF_KEY = "messaging.registration.id"
  private val PROPERTY_APP_VERSION = "appVersion"
  private val SENDER_ID = "122390040180"

  def registerId(ctx: Context, id: String): Future[String] = serverMessenger.registerId(ctx, id)

  def unregisterId(ctx: Context, id: String): Future[String] = serverMessenger.registerId(ctx, id)

  def tryRegister(activity: Activity): Future[String] = {
    if (checkPlayServices(activity)) {
      val regIdOpt = registrationId(activity)
      regIdOpt.fold(registerAndSave(activity))(Future.successful)
    } else {
      val msg = "No valid Google Play Services APK found."
      warn(msg)
      Future.failed(new MessagingException(msg))
    }
  }

  def unregister(ctx: Context): Future[String] =
    withRegistration(ctx, id => unregisterId(ctx, id)).map(id => {
      val preferences = prefs(ctx)
      preferences.edit().remove(REG_PREF_KEY).commit()
      id
    })

  def isRegistered(ctx: Context): Future[Boolean] = Future.successful(registrationId(ctx).isDefined)

  import GooglePlayServicesUtil._

  /** May display a Dialog that allows users to download the Play Services APK.
    *
    * @param activity
    * @return true if Google Play Services are available on the device, false otherwise
    */
  private def checkPlayServices(activity: Activity): Boolean = {
    val resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity)
    val isAvailable = resultCode == ConnectionResult.SUCCESS
    if (!isAvailable) {
      if (isUserRecoverableError(resultCode)) {
        GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show()
      } else {
        warn("This device does not support Google Play Services.")
        activity.finish()
      }
    }
    isAvailable
  }

  private def registerAndSave(ctx: Context): Future[String] = {
    withRegistration(ctx, registerId(ctx, _)).map(id => {
      saveRegistration(ctx, id)
      id
    })
  }

  private def withRegistration(ctx: Context, f: String => Future[String], then: GoogleCloudMessaging => Unit = _ => ()): Future[String] = {
    val gcm = GoogleCloudMessaging.getInstance(ctx)
    Future(gcm register SENDER_ID).flatMap(f)
  }

  private def registrationId(ctx: Context): Option[String] =
    loadRegistration(ctx).filter(_ => !hasAppBeenUpdated(ctx))

  private def hasAppBeenUpdated(ctx: Context): Boolean = {
    val savedVersionCode = prefs(ctx).getInt(PROPERTY_APP_VERSION, Int.MinValue)
    val versionCode = appVersion(ctx)
    savedVersionCode != versionCode
  }

  private def appVersion(ctx: Context) = ctx.getPackageManager.getPackageInfo(ctx.getPackageName, 0).versionCode

  private def saveRegistration(ctx: Context, id: String): Unit = {
    val versionCode = appVersion(ctx)
    val preferences = prefs(ctx)
    preferences.put(REG_PREF_KEY, id)
    preferences.putInt(PROPERTY_APP_VERSION, versionCode)
  }

  private def loadRegistration(ctx: Context): Option[String] = prefs(ctx) get REG_PREF_KEY

  private def prefs(ctx: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  override def close(): Unit = {
    serverMessenger.client.close()
  }
}
