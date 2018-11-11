package org.musicpimp.iap

import android.app.Activity
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.malliina.android.iap.amazon.AmazonIapUtils
import com.malliina.android.iap.{ProductInfo, IapUtilsBase}
import com.malliina.concurrent.ExecutionContexts.cached
import org.musicpimp.iap.google.KeyedGoogleIapUtils
import org.musicpimp.iap.samsung.SamsungIapUtils
import org.musicpimp.usage.PimpUsageController
import org.musicpimp.util.Keys
import scala.concurrent.Future

class PimpIapUtils(val iapUtils: IapUtilsBase, premiumSku: String) {
  def purchasePremium(activity: Activity): Future[String] = iapUtils.purchase(premiumSku, activity)

  /**
   * Syncs the premium status of the user with the app store and updates the settings accordingly as a side-effect. You
   * may want to run this when the app initially starts.
   *
   * @param activity context
   */
  def syncPremiumStatus(activity: Activity): Future[Boolean] = {
    val ret = iapUtils.hasSku(premiumSku, activity)
    ret.foreach(isPremium => {
      PimpIapUtils.savePremiumValue(PreferenceManager.getDefaultSharedPreferences(activity), isPremium)
    })
    ret
  }

  def premiumInfo(activity: Activity): Future[ProductInfo] = iapUtils.productInfo(premiumSku, activity)
}

object PimpIapUtils {
  def savePremiumValue(prefs: SharedPreferences, isPremium: Boolean): Unit = {
    PimpUsageController.allowUnlimited = isPremium
    prefs.edit().putBoolean(Keys.PREF_UNLIMITED_PLAYBACK, isPremium).apply()
  }
}

object GooglePimpIapUtils extends PimpIapUtils(KeyedGoogleIapUtils, IapInfo.unlimitedPlaybackSKU)

object AmazonPimpIapUtils extends PimpIapUtils(AmazonIapUtils, IapInfo.unlimitedPlaybackSKU)

object SamsungPimpIapUtils extends PimpIapUtils(SamsungIapUtils, SamsungIapUtils.premiumSku)
