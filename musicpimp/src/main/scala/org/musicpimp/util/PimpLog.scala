package org.musicpimp.util

import com.malliina.android.util.MleLog

trait PimpLog extends MleLog {
  // The logs of this app might be interleaved with logs from other apps so we better have an identifier
  override val tag = "MusicPimp"
}
