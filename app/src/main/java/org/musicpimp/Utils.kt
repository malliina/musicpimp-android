package org.musicpimp

import java.net.URLEncoder
import java.security.MessageDigest

object Utils {
    private const val hexChars = "0123456789abcdef"

    fun urlEncode(s: Primitive): String = URLEncoder.encode(s.value, "utf-8")

    fun hashString(input: String, type: String = "MD5"): String {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }
}
