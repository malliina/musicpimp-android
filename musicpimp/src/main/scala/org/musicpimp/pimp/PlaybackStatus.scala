package org.musicpimp.pimp

import org.musicpimp.audio.Track
import scala.concurrent.duration.Duration

case class PlaybackStatus(id: String,
                          title: String,
                          album: String,
                          artist: String,
                          durationDescribed: String,
                          duration: Duration,
                          posDescribed: String,
                          pos: Duration,
                          volume: Int,
                          playlist: Seq[Track],
                          playlistIndex: Option[Int])
