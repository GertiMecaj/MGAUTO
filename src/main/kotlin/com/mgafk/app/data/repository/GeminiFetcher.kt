package com.mgafk.app.data.repository

import com.mgafk.app.desktop.DesktopContext as Context
import com.mgafk.app.data.AppJson
import com.mgafk.app.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Downloads the latest [Gemini userscript](https://github.com/Ariedam64/Gemini/releases/latest)
 * The script is cached in memory for the current MGAUTO process only.
 * Nothing is written to disk or the Windows registry.
 */
object GeminiFetcher {

    private const val TAG = "GeminiFetcher"
    private const val RELEASES_URL = "https://api.github.com/repos/Ariedam64/Gemini/releases/latest"
    @Volatile private var cachedTag: String? = null
    @Volatile private var cachedScript: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = AppJson.default

    /** Current-process in-memory cache only. */
    fun readCached(context: Context): String? = cachedScript

    /**
     * Fetch the latest release. The downloaded script lives only in memory until MGAUTO exits.
     */
    suspend fun fetchLatest(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val metaReq = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            val metaRes = client.newCall(metaReq).execute()
            if (!metaRes.isSuccessful) {
                AppLog.w(TAG, "releases/latest HTTP ${metaRes.code}, using memory cache if any")
                return@withContext cachedScript
            }
            val body = metaRes.body?.string()
                ?: return@withContext cachedScript

            val root = json.parseToJsonElement(body) as? JsonObject
                ?: return@withContext cachedScript
            val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull
            val assets = root["assets"] as? JsonArray
            val downloadUrl = assets
                ?.mapNotNull { it as? JsonObject }
                ?.firstOrNull { (it["name"]?.jsonPrimitive?.contentOrNull ?: "").endsWith(".user.js") }
                ?.get("browser_download_url")?.jsonPrimitive?.contentOrNull

            if (tag == null || downloadUrl == null) return@withContext cachedScript
            if (tag == cachedTag && !cachedScript.isNullOrBlank()) return@withContext cachedScript

            val dlReq = Request.Builder().url(downloadUrl).build()
            val dlRes = client.newCall(dlReq).execute()
            if (!dlRes.isSuccessful) return@withContext cachedScript
            val script = dlRes.body?.string() ?: return@withContext cachedScript

            cachedTag = tag
            cachedScript = script
            AppLog.d(TAG, "Downloaded Gemini $tag (${script.length} chars) to memory")
            script
        } catch (e: Exception) {
            AppLog.w(TAG, "fetchLatest failed: ${e.message}, using memory cache if any")
            cachedScript
        }
    }

}
