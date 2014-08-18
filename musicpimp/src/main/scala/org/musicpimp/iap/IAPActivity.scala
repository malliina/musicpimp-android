package org.musicpimp.iap

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.mle.android.iap.IapException
import com.mle.android.ui.Implicits.action2clickListener
import com.mle.util.Utils.executionContext
import org.musicpimp.ui.activities.PreferenceListeningActivity
import org.musicpimp.util.{Keys, PimpLog}
import org.musicpimp.{R, TR}

import scala.concurrent.Future

/**
 *
 * @author mle
 */
abstract class IAPActivity extends PreferenceListeningActivity with PimpLog {
  def loadUnlimitedPlaybackAllowed: Boolean = prefs.getBoolean(Keys.PREF_UNLIMITED_PLAYBACK, false)

  override val contentView = R.layout.iap

  def storeDrawable: Int

  def iapUtils: PimpIapUtils

  def errorMessage: PartialFunction[Throwable, String] = defaultErrorMessage

  lazy val defaultErrorMessage: PartialFunction[Throwable, String] = {
    case ae: IapException =>
      warn(ae.getMessage, ae)
      ae.getMessage
    case t: Throwable =>
      warn("In-app billing failure", t)
      getResources.getString(R.string.generic_error_try_again)
  }

  lazy val displayError = errorMessage andThen setIabErrorText

  override protected def onCreate2(state: Option[Bundle]): Unit = {
    findView(TR.iab_purchase_button).setOnClickListener((view: View) => onPurchaseClicked(view))
    val localSkuStatus = loadUnlimitedPlaybackAllowed
    info(s"Has Premium? Local cache: $localSkuStatus")
    try {
      iapUtils.syncPremiumStatus(this).flatMap(hasPremium => {
        info(s"Has Premium? AppStore: $hasPremium")
        loadFeedback(hasPremium).map(fb => (hasPremium, fb))
      }).map(pair => updateUI(pair._1, pair._2)).onFailure(displayError)
    } catch {
      case t: Throwable =>
        // of course this should not be needed
        setIabErrorText(s"An error occurred. ${failMessage(t, stackTrace = false)}")
    }
//    updateUI(hasPremium = false)
  }

  def loadFeedback(hasPremium: Boolean) =
    if (hasPremium) Future.successful("You own MusicPimp Premium. Thank you!")
    else iapUtils.premiumInfo(this).map(i => s"Currently, only a limited number of tracks can be played per day. MusicPimp Premium is available and enables unlimited playback for ${i.price}.")

  def purchaseUnlimitedPlayback(): Unit =
    iapUtils.purchasePremium(this)
      .map(onPurchaseSucceeded)
      .recover(displayError)

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key == Keys.PREF_UNLIMITED_PLAYBACK) {
      val newValue = sharedPreferences.getBoolean(Keys.PREF_UNLIMITED_PLAYBACK, false)
      info(s"Premium status changed to: $newValue")
      updateUI(hasPremium = newValue)
    }
  }

  def updateUI(hasPremium: Boolean, feedback: String): Unit = onUiThread {
    setIabText(feedback)
    updateButton(hasPremium)
  }

  def updateUI(hasPremium: Boolean) = onUiThread {
    setIabText(feedback(hasPremium))
    updateButton(hasPremium)
  }

  private def updateButton(hasPremium: Boolean): Unit = onUiThread {
    val purchaseVisibility = if (hasPremium) View.INVISIBLE else View.VISIBLE
    findPurchaseButton.foreach(button => {
      button setVisibility purchaseVisibility
      // sets the drawableLeft of the button to storeDrawable (different icon for google play, amazon, samsung, ...)
      button.setCompoundDrawablesWithIntrinsicBounds(storeDrawable, 0, 0, 0)
    })
  }

  def feedback(hasSku: Boolean): Int =
    if (hasSku) R.string.premium_ok_thanks
    else R.string.playback_limit_desc

  def setIabText(text: String): Unit =
    findIabTextView.foreach(tv => onUiThread(tv setText text))

  def setIabText(textRes: Int): Unit =
    setIabText(getResources.getString(textRes))

  def setIabErrorText(text: String): Unit = onUiThread {
    warn(s"In-app billing error: $text")
    setIabText(text)
    // hides all other views
    findPurchaseButton.foreach(_ setVisibility View.INVISIBLE)
  }

  def findIabTextView = activityHelper.tryFindView(TR.iab_result)

  def findPurchaseButton = activityHelper.tryFindView(TR.iab_purchase_button)

  def onPurchaseClicked(view: View): Unit = purchaseUnlimitedPlayback()

  def onPurchaseSucceeded(sku: String) {
    info("The purchase task completed successfully, removing playback limits...")
    Option(prefs).foreach(p => {
      PimpIapUtils.savePremiumValue(prefs, isPremium = true)
      // onSharedPreferenceChanged is not called, why? consequently this updates the UI manually
      updateUI(hasPremium = true)
      info("User purchased unlimited playback. Removed playback limits.")
    })
  }

}
