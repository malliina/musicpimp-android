package org.musicpimp

import android.content.Context
import org.musicpimp.audio.Player
import org.musicpimp.backend.EmptyLibrary
import org.musicpimp.backend.Library
import org.musicpimp.backend.PimpLibrary
import org.musicpimp.backend.PimpSocket
import org.musicpimp.media.*
import org.musicpimp.ui.player.CoverService

class PimpComponents(appContext: Context) {
    var library: Library = EmptyLibrary.instance
    var playerSocket: PimpSocket? = null
    val covers = CoverService(appContext)
    val localPlayer: LocalPlayer = LocalPlayer(appContext)
    var player: Player = localPlayer
    val authHeader: HeaderValue?
        get() = library.client?.authHeader
}
