package org.musicpimp.audio

import rx.lang.scala.Subscription

/**
 * @author Michael
 */
trait PlayerEventListening {
  protected var playerEventSubscription: Option[Subscription] = None

  protected def onPlayerEvent(event: PlayerEvent): Unit

  protected def latestPlayer: Player

  def subscribeToPlayerEvents(): Unit =
    playerEventSubscription = Some(latestPlayer.events.subscribe(e => onPlayerEvent(e)))

  def unsubscribeFromPlayerEvents(): Unit = {
    playerEventSubscription.foreach(_.unsubscribe())
    playerEventSubscription = None
  }
}
