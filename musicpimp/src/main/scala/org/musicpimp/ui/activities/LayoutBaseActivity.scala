package org.musicpimp.ui.activities

import android.app.Activity
import android.os.Bundle
import android.widget.ListView
import org.musicpimp.TypedResource
import org.musicpimp.andro.ui.ActivityHelper

/**
 *
 * @author mle
 */
trait LayoutBaseActivity extends Activity {

  lazy val activityHelper = new ActivityHelper(this)

  def contentView: Int

  //  override def onCreate(savedInstanceState: Bundle): Unit = {
  //    super.onCreate(savedInstanceState)
  //    setContentView(contentView)
  //  }
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(contentView)
    onCreate2(Option(savedInstanceState))
  }

  /**
   * Convenience alternative to `onCreate`: super has already been called and the parameter is wrapped in an
   * [[scala.Option]] and guaranteed not to be null.
   *
   * @param state the state, wrapped in an [[scala.Option]]
   */
  protected def onCreate2(state: Option[Bundle]): Unit = {}

  def prefs = activityHelper.prefs

  def onUiThread(f: => Any): Unit = activityHelper.onUiThread(f)

  def extras = Option(getIntent.getExtras)

  def findView[A](tr: TypedResource[A]): A = activityHelper.findView(tr)

  def itemAt[T](index: Int, listResource: TypedResource[ListView]): T =
    findView(listResource).getAdapter.getItem(index).asInstanceOf[T]

  def navigate[T <: Activity](destActivity: Class[T], parameters: (String, String)*) =
    activityHelper.navigate(destActivity, parameters: _*)
}
