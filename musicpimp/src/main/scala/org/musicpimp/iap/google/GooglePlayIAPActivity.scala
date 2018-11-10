package org.musicpimp.iap.google

import android.content.Intent
import android.os.Bundle
import com.android.iab.util.IabHelper
import com.mle.android.iap.google._
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.R
import org.musicpimp.iap.google.GooglePlayIAPActivity._
import org.musicpimp.iap.{GooglePimpIapUtils, IAPActivity, IapInfo}
import org.musicpimp.util.PimpLog

class GooglePlayIAPActivity extends IAPActivity with PimpLog {
  var iabHelper: Option[AsyncIabHelper] = None
  val iapUtils = GooglePimpIapUtils

  def storeDrawable: Int = R.drawable.google_play

  override lazy val errorMessage: PartialFunction[Throwable, String] =
    googleErrorMessage orElse defaultErrorMessage

  def googleErrorMessage: PartialFunction[Throwable, String] = {
    case ire: IabResultException =>
      ire.result.getResponse match {
        case IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED =>
          str(R.string.user_canceled)
        case IabHelper.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE =>
          str(R.string.item_unavailable)
        case i if i > 0 && i <= 8 =>
          str(R.string.check_connection)
        case _ =>
          s"Failure: ${ire.getMessage}"
      }
    case ie: GooglePlayException =>
      warn("Google Play error", ie.throwable)
      str(R.string.google_play_missing)
    case pme: PayloadMismatchException =>
      warn("Payload mismatch", pme)
      str(R.string.payload_mismatch_info)
  }

  def str(resId: Int) = getResources getString resId

  override protected def onCreate2(state: Option[Bundle]): Unit = {
    super.onCreate2(state)
    val iab = new AsyncIabHelper(this, new IabHelper(this, publicKey))
    iabHelper = Some(iab)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    // delegates result handling to the helper, which may complete some future
    val isResultHandled = iabHelper
      .filter(_.iabHelper.handleActivityResult(requestCode, resultCode, data))
      .isDefined
    if (!isResultHandled) {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override def purchaseUnlimitedPlayback(): Unit = {
    iabHelper.fold(setIabText(R.string.google_play_error))(iab => {
      iab.startSetup.flatMap(_ => iab.purchase(this, IapInfo.unlimitedPlaybackSKU, 1000))
        .map(p => onPurchaseSucceeded(p.getSku))
        .recover(displayError)
    })
  }

  override def onDestroy() {
    super.onDestroy()
    iabHelper foreach (_.close())
    iabHelper = None
  }
}

object GooglePlayIAPActivity {

  private def keyParts = Seq(
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwoZ79Ngt/",
    "3xqFyacUhrLB4w8NWeuvxGBzpzYfp/s4GXAyZvSnYxccTXKFmSYI4eA3EB0E4hW3QO3",
    "OU2Rn5EIqloG9k7jrfE5RW0voE64KvtzpliQDbpr2C0OVz3kP8POxlal9IhtIcVp/",
    "sfCBgjClYr/Y3To11yFdKSWSoHEYRrk10rd8RfXxu9zW0w7SND15plJ1IVuDM+B9rXTshvg3ad4c5w69YlwW0ia1+hAdSpxvECc/",
    "7Fgne6toLKIWNc4vbI2siD7VxCSl2euVEY7hJDZan6N5OS3s/",
    "z9ITqS5jeo2MxKT2qJ4izFJ4CcBP2JGKyGQhq9tE3sMjO2TFbw/wIDAQAB"
  )

  def publicKey = keyParts.mkString
}