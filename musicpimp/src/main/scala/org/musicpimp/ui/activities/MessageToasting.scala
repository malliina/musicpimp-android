package org.musicpimp.ui.activities

import android.widget.Toast
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.util.{BasicMessage, PlaybackLimitExceeded, Reload, UIMessage}

/**
 * Toasts messages sent through [[org.musicpimp.util.Messaging]].
 *
 * @author mle
 */
trait MessageToasting extends MessageHandlerActivity {
  private lazy val help = new ActivityHelper(this)
  protected val handler: PartialFunction[UIMessage, Unit] = {
    case BasicMessage(msg) => help.showToast(msg, Toast.LENGTH_LONG)
    case PlaybackLimitExceeded => onPlaybackLimitExceeded()
  }

  def onPlaybackLimitExceeded(): Unit = ()
}

trait ReloadListening extends MessageHandlerFragment {
  protected val handler: PartialFunction[UIMessage, Unit] = {
    case Reload(silent) => onReload(silent)
  }

  def onReload(silent: Boolean): Unit
}