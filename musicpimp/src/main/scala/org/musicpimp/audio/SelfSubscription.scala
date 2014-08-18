package org.musicpimp.audio

import java.io.Closeable

/**
 * @author Michael
 */
trait SelfSubscription extends PlayerBase with Closeable {
  protected val subscription = events.subscribe(e => onPlayerEvent(e))
  protected var playerStatus: StatusEvent = StatusEvent.empty

  protected def onPlayerEvent(event: PlayerEvent): Unit

  def index: Option[Int] = playerStatus.playlistIndex

  def tracks: Seq[Track] = playerStatus.playlist

  def status: StatusEvent = playerStatus

  override def close(): Unit = {
    subscription.unsubscribe()
    super.close()
  }
}
