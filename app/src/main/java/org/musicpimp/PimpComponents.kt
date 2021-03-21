package org.musicpimp

import android.content.Context
import org.musicpimp.audio.Player
import org.musicpimp.backend.*
import org.musicpimp.media.*
import org.musicpimp.ui.player.CoverService

class PimpComponents(appContext: Context) {
    val http = HttpClient(appContext, emptyMap())
    var library: Library = EmptyLibrary.instance
    var playerSocket: PimpSocket? = null
    val covers = CoverService(appContext)
    val localPlayer: LocalPlayer = LocalPlayer(appContext)
    var player: Player = localPlayer
    var authHeader: HeaderValue? = null
}
