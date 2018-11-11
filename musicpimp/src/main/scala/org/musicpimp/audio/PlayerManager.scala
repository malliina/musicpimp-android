package org.musicpimp.audio

import com.malliina.android.exceptions.ExplainedException
import org.musicpimp.beam.BeamPlayer
import org.musicpimp.http.Endpoint
import org.musicpimp.local.{LimitedLocalPlayer, LocalPlayer}
import org.musicpimp.pimp._
import org.musicpimp.subsonic.SubsonicPlayer
import org.musicpimp.usage.{BeamLimiter, PimpLimiter, PimpUsageController, SubsonicLimiter}
import org.musicpimp.util.{Keys, PimpLog}
import scala.concurrent.ExecutionContext.Implicits.global

trait PlayerManager extends EndpointManager[Player] with PimpLog {
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
    import org.musicpimp.http.EndpointTypes._
    val isPremium = PimpUsageController.allowUnlimited
    val p = e.endpointType match {
      case Local =>
        localPlayer
      case MusicPimp =>
        if (isPremium) new PimpServerPlayer(e)
        else new PimpServerPlayer(e) with PimpLimiter
      case Cloud =>
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
    p.open()
    p
  }
}

object PlayerManager extends PlayerManager
