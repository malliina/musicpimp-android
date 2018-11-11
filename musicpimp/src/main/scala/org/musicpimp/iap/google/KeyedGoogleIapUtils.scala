package org.musicpimp.iap.google

import com.malliina.android.iap.google.GoogleIapUtils

object KeyedGoogleIapUtils extends GoogleIapUtils {
  def publicKey: String = GooglePlayIAPActivity.publicKey
}
