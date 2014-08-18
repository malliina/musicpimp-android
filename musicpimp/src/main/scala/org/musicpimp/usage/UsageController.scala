package org.musicpimp.usage


/**
 * Users may perform an in-app purchase to enable unlimited playback.
 *
 * Until the user has purchased, the following upper limits apply:
 *
 * Local playback: 3 tracks / 24 hours
 * Remote playback: 3 tracks / 24 hours
 *
 * To enforce the limits, we record the timestamp every time playback
 * of a track starts. On the fourth playback attempt, if the three
 * latest timestamps are all within 24 hours of the current time,
 * usage is not allowed.
 *
 * @author mle
 */
trait UsageController {
  def allowUnlimited: Boolean

  def isLocalPlaybackAllowed: Boolean

  def isRemotePlaybackAllowed: Boolean

  def remotePlaybackStarted(): Unit

  def localPlaybackStarted(): Unit
}

