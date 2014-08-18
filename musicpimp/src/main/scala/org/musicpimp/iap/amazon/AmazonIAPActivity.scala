package org.musicpimp.iap.amazon

import org.musicpimp.R
import org.musicpimp.util.PimpLog
import org.musicpimp.iap.{AmazonPimpIapUtils, IAPActivity}

/**
 * @author mle
 */
class AmazonIAPActivity extends IAPActivity with PimpLog {
  val iapUtils = AmazonPimpIapUtils

  def storeDrawable: Int = R.drawable.amazon
}
