package com.mgafk.app.desktop

import com.mgafk.app.data.AppJson
import com.mgafk.app.data.repository.GeminiFetcher
import com.mgafk.app.data.websocket.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

object BrowserHost {
    suspend fun open(mode: String, profile: String, url: String = "https://magicgarden.gg", cookie: String = "", inject: Boolean = false): String? = withContext(Dispatchers.IO) {
        val resources = System.getProperty("compose.application.resources.dir")
        val executable = listOfNotNull(resources?.let { File(it, "browser/MGAUTO.Browser.exe") }, File("resources/windows/browser/MGAUTO.Browser.exe")).firstOrNull { it.isFile }
            ?: error("Browser component is missing. Run the installed Windows build.")
        val script = if (inject) GeminiFetcher.fetchLatest(DesktopContext.instance) ?: error("Gemini could not be downloaded. Disable Inject Gemini mod to play without it, or try again.") else ""
        val request = buildJsonObject {
            put("Mode", mode); put("Profile", profile); put("Url", url); put("Cookie", cookie)
            put("Script", script); put("OAuth", Constants.DISCORD_OAUTH_URL)
        }
        val process = ProcessBuilder(executable.absolutePath).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        // Session credentials travel through a private child-process pipe, never command-line arguments.
        process.outputStream.bufferedWriter().use { it.write(request.toString()); it.newLine() }
        var token: String? = null
        var failure: String? = null
        process.inputStream.bufferedReader().useLines { lines -> lines.forEach { line ->
            val data = runCatching { AppJson.default.parseToJsonElement(line).jsonObject }.getOrNull()
            data?.get("token")?.jsonPrimitive?.contentOrNull?.let { token = it }
            data?.get("error")?.jsonPrimitive?.contentOrNull?.let { failure = it }
        } }
        val exit = process.waitFor()
        if (failure != null || exit != 0) error(failure ?: "Browser exited with code $exit")
        token
    }
}
