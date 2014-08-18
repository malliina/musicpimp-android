package org.musicpimp.ui.fragments

import android.support.v4.app.Fragment
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.audio._

/**
 *
 * @author mle
 */
trait PlaybackFragment extends Fragment with PlaybackListening {
  lazy val activityHelper = new ActivityHelper(getActivity)
  // needed so we can remove the event handler when needed
  protected var latestPlayer: Player = PlayerManager.active

  protected def onPlayerEvent(event: PlayerEvent): Unit

  protected def onPlayerChangedEvent(event: Changed): Unit = {
    unsubscribeFromPlayerEvents()
    resetUI()
  }

  /**
   * Called when the player has changed. This means both the player and the playlist should be redrawn.
   */
  def resetUI(): Unit

  override def onResume(): Unit = {
    subscribeToPlayerChangedEvents()
    super.onResume()
  }

  override def onPause(): Unit = {
    unsubscribeFromPlayerChangedEvents()
    super.onPause()
  }
}
