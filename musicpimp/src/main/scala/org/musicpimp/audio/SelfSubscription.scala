package org.musicpimp.audio

import java.io.Closeable

import org.musicpimp.util.PimpLog

trait SelfSubscription extends PlayerBase with Closeable with PimpLog {
  protected val subscription = events.subscribe(e => onPlayerEvent(e), err => warn("Player failure", err))
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
