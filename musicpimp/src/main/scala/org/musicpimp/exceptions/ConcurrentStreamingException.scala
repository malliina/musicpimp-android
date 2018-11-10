package org.musicpimp.exceptions

import org.musicpimp.audio.Track
import com.mle.android.exceptions.ExplainedException

class ConcurrentStreamingException(track: Track)
  extends ExplainedException(s"Cannot add ${track.title} to MusicBeamer. It appears another track is being concurrently streamed to the player. Try again later.")