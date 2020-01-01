package org.musicpimp

import android.content.Context
import org.musicpimp.audio.Player
import org.musicpimp.backend.PimpHttpClient
import org.musicpimp.backend.PimpSocket
import org.musicpimp.media.LocalPlayer
import org.musicpimp.media.LocalPlaylist
import org.musicpimp.media.PimpMediaBrowser
import org.musicpimp.media.MusicService

class PimpComponents(context: Context) {
    val local = LocalPlayer(PimpMediaBrowser(context, MusicService::class.java), LocalPlaylist())
    var http: PimpHttpClient? = null
    var playerSocket: PimpSocket? = null
    var player: Player = local

    val authHeader: AuthHeader?
        get() = http?.http?.authHeader
}
