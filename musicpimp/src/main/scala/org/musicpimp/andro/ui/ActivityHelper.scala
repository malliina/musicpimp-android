package org.musicpimp.andro.ui

import android.app.Activity
import android.content.Intent
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import com.mle.util.Utils
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.musicpimp.util.PimpLog
import org.musicpimp.{TypedFindView, TypedResource}

/**
 * @author Michael
 */
class ActivityHelper(val activity: Activity) extends TypedFindView with PimpLog {
  override def findViewById(id: Int): View = activity.findViewById(id)

  def prefs = PreferenceManager.getDefaultSharedPreferences(activity)

  def tryFindIntView[A](id: Int): Option[A] = Option(activity findViewById id).map(_.asInstanceOf[A])

  /**
   * Returns the view with the given id if this fragment is attached and the view is found.
   *
   * Both activity and findViewById may return null at arbitrary times, therefore the result is wrapped in an Option.
   *
   * @param tr resource id
   * @tparam A type of view
   * @return the view wrapped in an Option, or None if it could not be obtained
   */
  def tryFindView[A](tr: TypedResource[A]): Option[A] = tryFindIntView[A](tr.id)

  def navigate[T <: Activity](destActivity: Class[T], parameters: (String, String)*) {
    val intent = new Intent(activity, destActivity)
    parameters foreach {
      case (key, value) => intent.putExtra(key, value)
    }
    activity startActivity intent
  }

  def navigateForResult[T <: Activity](destActivity: Class[T], requestCode: Int) {
    val intent = new Intent(activity, destActivity)
    activity startActivityForResult(intent, requestCode)
  }

  def onUiThread(f: => Any): Unit = activity.runOnUiThread(Utils.runnable(f))

  def showToast(text: String, duration: Int = Toast.LENGTH_LONG): Unit =
    onUiThread(Toast.makeText(activity, text, duration).show())

  def showToast(stringRes: Int): Unit = showToast(activity.getResources getString stringRes)

  /**
   * Executes the action, showing a toast if the action throws an exception.
   *
   * @param default return value for when the action throws an exception
   * @param action code to execute
   * @tparam T return type
   * @return the return value of `action`, or `default` if `action` throws an exception
   */
  def toastOnException[T](default: T)(action: => T): T =
    try {
      action
    } catch {
      case wse: WebsocketNotConnectedException =>
        showToast("Unable to perform action. The connection to the server has been lost.")
        default
      case e: Exception =>
        warn(s"Failure", e)
        showToast("Unable to perform action. Please check your settings and try again.")
        default
    }

  /**
   * Executes the action, showing a toast if it throws an exception.
   *
   * @param action the code to execute
   * @return true, regardless of whether the action throws an exception or not
   */
  def trueToastException(action: => Unit): Boolean = toastOnException(default = true) {
    action
    true
  }
}
