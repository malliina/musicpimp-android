package org.musicpimp.backend

import org.musicpimp.FullUrl

class Env {
    companion object {
        private const val BackendDomain = "cloud.musicpimp.org"
        val baseUrl = FullUrl.https(BackendDomain, "")
//        private const val BackendDomain = "10.0.0.13:8456"
//        val baseUrl = FullUrl.http(BackendDomain, "")
//        val socketsUrl = FullUrl.wss(BackendDomain, "/ws/updates")
    }
}
