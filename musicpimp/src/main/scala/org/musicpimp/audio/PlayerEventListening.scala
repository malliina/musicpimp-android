package org.musicpimp.audio

import org.musicpimp.util.PimpLog
import rx.lang.scala.Subscription

trait PlayerEventListening extends PimpLog {
  protected var playerEventSubscription: Option[Subscription] = None

  protected def onPlayerEvent(event: PlayerEvent): Unit

  protected def latestPlayer: Player

  def subscribeToPlayerEvents(): Unit =
    playerEventSubscription = Some(latestPlayer.events.subscribe(
      e => onPlayerEvent(e),
      err => warn("Player event failure.", err)
    ))

  def unsubscribeFromPlayerEvents(): Unit = {
    playerEventSubscription.foreach(_.unsubscribe())
    playerEventSubscription = None
  }
}
