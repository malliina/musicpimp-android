package org.musicpimp.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.malliina.android.ui.Implicits.{action2clickListener, fun2seekChangeListener}
import concurrent.duration._
import org.musicpimp.audio._
import org.musicpimp.ui.Assets
import org.musicpimp.ui.activities.VolumeActivity
import org.musicpimp.{TR, R}

class PlayerFragment extends PlaylistControls {
  override val layoutId: Int = R.layout.player

  override def onPlayerEvent(event: PlayerEvent) = event match {
    case TimeUpdated(time) =>
      //      info(s"Got time updated: $time")
      updateTrackPosition(time)
    case TrackChanged(track) =>
      fillTrackInfo(track)
    case PlaylistModified(playlist) =>
      updateNextTrack(player.index, playlist)
      findListView.foreach(listView => showPlaylistItems(listView, playlist))
    case PlaylistIndexChanged(index) =>
      updateNextTrack(index, player.tracks)
      highlightPlaylistItem(index)
    case _ => ()
  }

  override def initViews(savedInstanceState: Option[Bundle]): Unit = {
    super.initViews(savedInstanceState)
    findSeekBar.foreach(_.setOnSeekBarChangeListener((_: SeekBar, pos: Int) => player.seek(pos seconds)))
    findVolumeButton.foreach(volumeButton => {
      volumeButton.setOnClickListener(() => activityHelper.navigate(classOf[VolumeActivity]))
      volumeButton setTypeface Assets.fontAwesome
    })
  }

  def updateNextTrack(currentIndex: Option[Int], playlist: Seq[Track]): Unit =
    updateNextTrack(currentIndex.filter(_ + 1 < playlist.size).map(i => playlist(i + 1)))

  def updateNextTrack(trackOpt: Option[Track]): Unit = {
    for {
      nextViews <- activityHelper.tryFindView(TR.`next_track_views`)
      nextTitle <- activityHelper.tryFindView(TR.`next_track_title`)
      nextArtist <- activityHelper.tryFindView(TR.`next_track_artist`)
    } yield {
      val visibility = trackOpt.fold(View.INVISIBLE)(_ => View.VISIBLE)
      activityHelper.onUiThread {
        nextViews setVisibility visibility
        trackOpt.foreach(track => {
          activityHelper.findView(TR.`next_track_title`) setText track.title
          activityHelper.findView(TR.`next_track_artist`) setText track.artist
        })
      }
    }
  }

  def updateTrackPosition(position: Duration): Unit = activityHelper.onUiThread {
    findSeekBar.foreach(_ setProgress position.toSeconds.toInt)
    findTrackPositionText.foreach(_ setText describe(position))
  }

  def fillTrackInfo(trackOpt: Option[Track]): Unit = activityHelper.onUiThread {
    for {
      playerViews <- playerControls
      noTrackText <- findNoTrackText
      title <- findTitleText
      album <- activityHelper.tryFindView(TR.track_album)
      artist <- activityHelper.tryFindView(TR.track_artist)
      position <- findTrackPositionText
      duration <- activityHelper.tryFindView(TR.track_duration)
      seekBar <- findSeekBar
    } yield {
      trackOpt.fold({
        playerViews setVisibility View.INVISIBLE
        noTrackText setVisibility View.VISIBLE
      })(track => {
        playerViews setVisibility View.VISIBLE
        noTrackText setVisibility View.GONE

        title setText track.title
        album setText track.album
        artist setText track.artist
        position setText describe(Duration.fromNanos(0))
        duration setText describe(track.duration)
        seekBar setProgress 0
        seekBar setMax track.duration.toSeconds.toInt
      })
    }
  }

  override def redraw() {
    super.redraw()
    val track = player.currentTrack
    fillTrackInfo(track)
    updateTrackPosition(player.status.position)
    updateNextTrack(player.index, player.tracks)
    findSeekBar.foreach(_.setEnabled(player.supportsSeekAndSkip))
  }

  def findNoTrackText = activityHelper.tryFindView(TR.no_track_text)

  def findSeekBar = activityHelper.tryFindView(TR.seekbar)

  def findTitleText = activityHelper.tryFindView(TR.track_title)

  def findVolumeButton = activityHelper.tryFindView(TR.volume_text_button)

  def findTrackPositionText = activityHelper.tryFindView(TR.track_position)

  def playerControls = activityHelper.tryFindView(TR.player_controls)

  /**
   * Converts a duration to a readable format.
   * Readable is defined as "00:00" if the duration is under 1 hour, otherwise "00:00:00".
   *
   * @param duration duration to describe
   * @return a date of format "00:00" or "00:00:00"
   */
  private def describe(duration: Duration): String = {
    val seconds = duration.toSeconds
    val secondsRemainder = seconds % 60
    val minutesRemainder = (seconds / 60) % 60
    val hours = seconds / 3600
    if (seconds >= 3600) "%02d:%02d:%02d".format(hours, minutesRemainder, secondsRemainder)
    else "%02d:%02d".format(minutesRemainder, secondsRemainder)
  }
}
