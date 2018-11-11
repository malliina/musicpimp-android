package org.musicpimp.audio

trait TrackListener extends PlaybackListening {
  def onTrackChanged(trackOpt: Option[Track])

  def onPlayStateChanged(state: PlayStates.PlayState)

  protected var latestPlayer: Player = playerManager.active

  override protected def onPlayerEvent(event: PlayerEvent) = event match {
    case TrackChanged(trackOpt) => onTrackChanged(trackOpt)
    case PlayStateChanged(state) => onPlayStateChanged(state)
    case _ => ()
  }

  override protected def onPlayerChangedEvent(event: Changed) = {
    unsubscribeFromPlayerEvents()
    onDeactivated(latestPlayer)
    latestPlayer = player
    subscribeToPlayerEvents()
    onActivated(latestPlayer)
  }

  def playerManager: PlayerManager = PlayerManager

  def player = playerManager.active

  def onActivated(player: Player): Unit = ()

  def onDeactivated(player: Player): Unit = ()

  def registerPlayerEventsListener() {
    subscribeToPlayerChangedEvents()
    subscribeToPlayerEvents()
  }

  def unregisterPlayerEventsListener() {
    unsubscribeFromPlayerChangedEvents()
    unsubscribeFromPlayerEvents()
  }
}
