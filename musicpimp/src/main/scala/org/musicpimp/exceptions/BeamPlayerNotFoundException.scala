package org.musicpimp.exceptions

import com.mle.android.exceptions.ExplainedException

class BeamPlayerNotFoundException extends ExplainedException("The connection to the MusicBeamer player has been lost. Please review your playback settings.")
