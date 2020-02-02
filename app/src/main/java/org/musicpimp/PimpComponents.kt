package org.musicpimp

import android.content.Context
import org.musicpimp.audio.Player
import org.musicpimp.backend.PimpLibrary
import org.musicpimp.backend.PimpSocket
import org.musicpimp.media.*
import org.musicpimp.ui.player.CoverService

class PimpComponents(context: Context) {
//    val local = LocalPlayer(PimpMediaBrowser(context, MusicService::class.java), LocalPlaylist())
    var library: PimpLibrary? = null
    var playerSocket: PimpSocket? = null
    val covers = CoverService(context)
    val localPlayer: SimplePlayer = SimplePlayer(context, covers)
    var player: Player = localPlayer
    val authHeader: AuthHeader?
        get() = library?.http?.authHeader
}
