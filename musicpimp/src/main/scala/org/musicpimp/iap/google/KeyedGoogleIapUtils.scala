package org.musicpimp.iap.google

import com.mle.android.iap.google.GoogleIapUtils

/**
 *
 * @author mle
 */
object KeyedGoogleIapUtils extends GoogleIapUtils {
  def publicKey: String = GooglePlayIAPActivity.publicKey
}
