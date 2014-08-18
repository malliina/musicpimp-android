package org.musicpimp.ui.activities

import android.os.Bundle
import android.widget.SeekBar
import com.mle.android.ui.Implicits._
import org.musicpimp.R
import org.musicpimp.TR
import org.musicpimp.audio.{PlayerManager, MuteToggled, VolumeChanged, PlayerEvent}
import org.musicpimp.ui.Assets
import rx.lang.scala.Subscription

/**
 *
 * @author mle
 */
class VolumeActivity extends LayoutBaseActivity with PlayerListeningActivity {
  protected var subscription: Option[Subscription] = None
  override val contentView = R.layout.volume

  var isMute = false
  var vol = 40

  def findVolumeBar = findView(TR.volume_bar)

  def findVolumeButton = findView(TR.volume_button)

  def latestPlayer = PlayerManager.active

  def onPlayerEvent(event: PlayerEvent) = event match {
    case VolumeChanged(volume) =>
      vol = volume
      updateVolume(volume)
    case MuteToggled(mute) =>
      isMute = mute
      updateVolumeImage(mute, vol)
    case _ => ()
  }

  def updateVolume(volume: Int) = onUiThread {
    findVolumeBar setProgress volume
    findVolumeButton setText determineImage(volume)
  }

  def updateVolumeImage(mute: Boolean, volume: Int): Unit = {
    val imageResource = if (mute) R.string.fa_volume_off else determineImage(volume)
    onUiThread {
      findVolumeButton setText imageResource
    }
  }

  def determineImage(volume: Int): Int =
    if (volume < 50) R.string.fa_volume_down
    else R.string.fa_volume_up


  override protected def onCreate2(savedInstanceState: Option[Bundle]): Unit = {
    findVolumeBar.setOnSeekBarChangeListener((bar: SeekBar, pos: Int) => latestPlayer.volume(pos))
    val volumeButton = findVolumeButton
    volumeButton setOnClickListener (() => latestPlayer.mute(!isMute))
    volumeButton setTypeface Assets.fontAwesome
  }

  override def onResume() {
    super.onResume()
    val status = latestPlayer.status
    isMute = status.mute
    vol = status.volume
    updateVolume(vol)
    updateVolumeImage(isMute, vol)
  }
}
