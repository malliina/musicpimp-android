package org.musicpimp.audio

import rx.lang.scala.Subscription

/**
 * @author Michael
 */
trait PlaybackListening extends PlayerEventListening {
  protected var playerChangedSubscription: Option[Subscription] = None

  protected def onPlayerChangedEvent(event: Changed): Unit

  def subscribeToPlayerChangedEvents(): Unit =
    playerChangedSubscription = Some(PlayerManager.events.subscribe((e: Changed) => onPlayerChangedEvent(e)))

  def unsubscribeFromPlayerChangedEvents(): Unit = {
    playerChangedSubscription.foreach(_.unsubscribe())
    playerChangedSubscription = None
  }
}
