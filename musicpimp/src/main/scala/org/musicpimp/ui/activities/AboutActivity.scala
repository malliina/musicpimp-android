package org.musicpimp.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.widget.{ListView, AdapterView}
import com.mle.andro.ui.adapters.TwoPartItem
import com.mle.android.ui.Implicits.action2itemClickListener2
import org.musicpimp.R.string._
import org.musicpimp.ui.adapters.FontAwesomeAdapter
import org.musicpimp.ui.dialogs.FirstUseWelcomeTutorial
import org.musicpimp.{PimpApp, TR, R}

/**
 *
 * @author mle
 */
class AboutActivity extends ActionBarActivity with LayoutBaseActivity {
  val items = Seq(
    TwoPartItem(fa_envelope_o, developed_by),
    TwoPartItem(fa_external_link, visit_pimp_org),
    TwoPartItem(fa_star_o, consider_reviewing),
    TwoPartItem(fa_question, view_tutorial),
    TwoPartItem(fa_shopping_cart, view_purchase_info)
  )

  override val contentView = R.layout.about

  override protected def onCreate2(savedInstanceState: Option[Bundle]) {
    val adapter = new FontAwesomeAdapter(this, items)

    val listView = findListView
    listView setAdapter adapter
    listView.setOnItemClickListener(onItemSelected _)
  }

  def onItemSelected(av: AdapterView[_], index: Int) {
    val item = (av getItemAtPosition index).asInstanceOf[TwoPartItem]
    import Intent._

    val (intentOpt, failureMessage) = item.secondResource match {
      case i: Int if i == developed_by =>
        // opens email intent to info@musicpimp.org
        val emailIntent = new Intent(ACTION_SEND)
        // PLAIN_TEXT_TYPE shows many non-email apps
        // message/rfc822 shows only gmail and bluetooth for me so it's at least better
        emailIntent setType "message/rfc822" // HTTP.PLAIN_TEXT_TYPE
        emailIntent putExtra(EXTRA_EMAIL, Array("info@musicpimp.org"))
        emailIntent putExtra(EXTRA_SUBJECT, "MusicPimp Feedback")
        emailIntent putExtra(EXTRA_TEXT, "Great app!")
        (Some(emailIntent), "Unable to find a suitable email app. Please consider opening your email app manually. Sorry for the inconvenience.")
      case i: Int if i == visit_pimp_org =>
        // opens browser intent to www.musicpimp.org
        val browserIntent = new Intent(ACTION_VIEW)
        browserIntent setData Uri.parse("http://www.musicpimp.org/")
        (Some(browserIntent), "Unable to find a suitable web browser.")
      case i: Int if i == consider_reviewing =>
        // opens link to marketplace
        val intent = new Intent(ACTION_VIEW)
        intent setData PimpApp.appStoreInfo.marketUri(getPackageName)
        (Some(intent), "Unable to find app marketplace.")
      case i: Int if i == view_tutorial =>
        new FirstUseWelcomeTutorial().show(getSupportFragmentManager, "tutorial-welcome")
        (None, "")
      case i: Int if i == view_purchase_info =>
        activityHelper.navigate(PimpApp.appStoreInfo.iapActivity)
        (None, "")
      case _ =>
        (None, "")
    }
    intentOpt.foreach(intent => startIntentIfAppExists(intent, failureMessage))
  }

  def startIntentIfAppExists(intent: Intent, failureMessage: String): Unit =
    if (existsAppToReceiveIntent(intent))
      startActivity(intent)
    else
      activityHelper.showToast(failureMessage)

  def existsAppToReceiveIntent(intent: Intent): Boolean =
    getPackageManager.queryIntentActivities(intent, 0).size() > 0

  def findListView = activityHelper.findView(TR.listView).asInstanceOf[ListView]
}
