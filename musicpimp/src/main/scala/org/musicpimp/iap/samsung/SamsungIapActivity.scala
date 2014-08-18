package org.musicpimp.iap.samsung

import org.musicpimp.iap.{SamsungPimpIapUtils, PimpIapUtils, IAPActivity}
import org.musicpimp.R

/**
 *
 * @author mle
 */
class SamsungIapActivity extends IAPActivity {
  override def iapUtils: PimpIapUtils = SamsungPimpIapUtils

  override def storeDrawable: Int = R.drawable.samsung
}
