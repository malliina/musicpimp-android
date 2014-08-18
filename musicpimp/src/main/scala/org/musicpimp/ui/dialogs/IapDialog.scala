package org.musicpimp.ui.dialogs

import com.mle.andro.ui.dialogs.DefaultDialog
import com.mle.android.ui.fragments.BaseFragment
import org.musicpimp.PimpApp
import org.musicpimp.R.string._
import android.support.v4.app.Fragment
import org.musicpimp.andro.ui.ActivityHelper

/**
 *
 * @author mle
 */
class IapDialog extends DefaultDialog(
  message = free_over_suggest_purchase,
  title = Some(limit_title),
  positiveText = Some(go_premium),
  negativeText = Some(not_interested)
) {
  private lazy val helper = new ActivityHelper(getActivity)

  override def onPositive(): Any = helper.navigate(PimpApp.appStoreInfo.iapActivity)
}