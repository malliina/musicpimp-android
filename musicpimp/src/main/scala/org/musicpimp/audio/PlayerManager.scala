package org.musicpimp.audio

import com.mle.android.exceptions.ExplainedException
import org.musicpimp.beam.BeamPlayer
import org.musicpimp.http.{EndpointTypes, Endpoint}
import org.musicpimp.local.{LimitedLocalPlayer, LocalPlayer}
import org.musicpimp.pimp._
import org.musicpimp.subsonic.SubsonicPlayer
import org.musicpimp.usage.{BeamLimiter, SubsonicLimiter, PimpLimiter, PimpUsageController}
import org.musicpimp.util.Keys

/**
 *
 * @author mle
 */
trait PlayerManager extends EndpointManager[Player] {
  val localPlayer =
    if (PimpUsageController.allowUnlimited) LocalPlayer
    else LimitedLocalPlayer

  override val default: Player = localPlayer
  override val prefKey: String = Keys.PREF_PLAYER
  override var active: Player = loadActive(prefs)

  /**
   * TODO return Future[Player] that completes when the player has been opened and fallback to LocalPlayer if opening fails.
   */
  override def buildEndpoint(e: Endpoint): Player = {
    import EndpointTypes._
    val isPremium = PimpUsageController.allowUnlimited
    val p = e.endpointType match {
      case Local =>
        localPlayer
      case MusicPimp =>
        if (isPremium) new PimpServerPlayer(e)
        else new PimpServerPlayer(e) with PimpLimiter
      case MusicBeamer =>
        if (isPremium) new BeamPlayer(e)
        else new BeamPlayer(e) with BeamLimiter
      case Subsonic =>
        if (isPremium) new SubsonicPlayer(e)
        else new SubsonicPlayer(e) with SubsonicLimiter
      case other =>
        throw new ExplainedException(s"Unsupported player endpoint: $other")
    }
    // TODO: exception handling
    p.open()
    p
  }
}

object PlayerManager extends PlayerManager
