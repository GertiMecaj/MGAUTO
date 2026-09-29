package com.mgafk.app.desktop

import java.net.URI
import java.net.URLEncoder

object GameLaunch {
    fun url(host: String, room: String): String {
        require(room.isNotBlank()) { "Enter a room before opening the game." }
        val supplied = host.trim().ifBlank { "magicgarden.gg" }
        val base = if (supplied.contains("://")) supplied else "https://$supplied"
        val parsed = URI(base)
        require(parsed.scheme == "https" && !parsed.host.isNullOrBlank() && parsed.userInfo == null) { "Enter a valid HTTPS game host." }
        val encoded = URLEncoder.encode(room.trim(), "UTF-8").replace("+", "%20")
        return "https://${parsed.rawAuthority}/r/$encoded"
    }
    fun cookie(token: String): String {
        require(token.isNotBlank() && !token.contains('\n') && !token.contains('\r') && !token.contains(';')) { "Invalid session token." }
        return if (token.startsWith("mc_jwt=")) token else "mc_jwt=$token"
    }
}
