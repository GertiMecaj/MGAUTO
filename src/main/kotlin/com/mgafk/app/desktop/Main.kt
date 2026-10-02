package com.mgafk.app.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.mgafk.app.ui.MainViewModel
import com.mgafk.app.ui.screens.MainScreen
import com.mgafk.app.ui.theme.MgAfkTheme
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.image.BufferedImage
import java.io.RandomAccessFile
import java.io.File
import java.nio.channels.FileLock
import java.util.prefs.Preferences
import javax.swing.JOptionPane

private object SingleInstanceGuard {
    private var file: RandomAccessFile? = null
    private var lock: FileLock? = null

    private val lockFile = File(
        File(System.getProperty("java.io.tmpdir"), "MGAUTO").apply { mkdirs() },
        "mgauto.instance.lock",
    )

    fun acquire(context: DesktopContext): Boolean {
        val handle = runCatching {
            RandomAccessFile(lockFile, "rw")
        }.getOrNull() ?: return false

        val acquired = runCatching { handle.channel.tryLock() }.getOrNull()
        if (acquired == null) {
            runCatching { handle.close() }
            return false
        }

        file = handle
        lock = acquired
        return true
    }

    fun release() {
        runCatching { lock?.release() }
        runCatching { file?.close() }
        lock = null
        file = null
        runCatching { lockFile.delete() }
    }
}

private fun clearLegacyTransientData(context: DesktopContext) {
    // Keep settings.preferences_pb only. Everything below is legacy/transient data.
    listOf(
        "crash_log.txt",
        "gemini.user.js",
        "nuclear-events.jsonl",
        "mgauto.instance.lock",
    ).forEach { name -> runCatching { File(context.filesDir, name).delete() } }

    runCatching { File(context.filesDir, "Browser").deleteRecursively() }
    context.filesDir.listFiles()
        ?.filter { it.name.startsWith("settings.preferences_pb.corrupt-") }
        ?.forEach { runCatching { it.delete() } }

    // Older Gemini builds stored a cache tag in the Windows Preferences registry.
    runCatching {
        val node = Preferences.userRoot().node("MGAUTO/gemini_fetcher")
        if (node.nodeExists("")) node.removeNode()
        Preferences.userRoot().flush()
    }
}

fun main(args: Array<String>) {
    val context = DesktopContext.instance
    if (!SingleInstanceGuard.acquire(context)) {
        JOptionPane.showMessageDialog(
            null,
            "MGAUTO is already running. Open it from the system tray instead of launching another copy.",
            "MGAUTO already running",
            JOptionPane.INFORMATION_MESSAGE,
        )
        return
    }

    clearLegacyTransientData(context)
    try {
    application {
        val model = remember { MainViewModel() }
        val scope = rememberCoroutineScope()
        var visible by remember { mutableStateOf(true) }
        val busyBrowsers = remember { mutableStateListOf<String>() }
        val state = rememberWindowState(width = 1180.dp, height = 850.dp)
        fun quit() { model.close(); exitApplication() }
        DisposableEffect(Unit) {
            var icon: TrayIcon? = null
            if (SystemTray.isSupported()) {
                val bitmap = BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB)
                bitmap.createGraphics().apply { color = Color(0x17202B); fillRoundRect(0,0,32,32,8,8); color = Color(0x6C8CFF); font = Font("SansSerif",Font.BOLD,22); drawString("M",6,25); dispose() }
                val menu = PopupMenu()
                menu.add(MenuItem("Open MGAUTO").apply { addActionListener { visible = true; state.isMinimized = false } })
                menu.add(MenuItem("Stop alarm").apply { addActionListener { DesktopAlerts.stop() } })
                menu.addSeparator()
                menu.add(MenuItem("Quit").apply { addActionListener { quit() } })
                icon = TrayIcon(bitmap,"MGAUTO — Magic Garden",menu).apply {
                    isImageAutoSize = true
                    addActionListener { visible = true; state.isMinimized = false }
                }
                runCatching { SystemTray.getSystemTray().add(icon) }; DesktopAlerts.tray = icon
            }
            onDispose { icon?.let { SystemTray.getSystemTray().remove(it) }; model.close() }
        }
        Window(title = "MGAUTO — Magic Garden", state = state, visible = visible, onCloseRequest = {
            val options = if (DesktopAlerts.tray != null) arrayOf("Keep running in tray", "Quit", "Cancel") else arrayOf("Quit", "Cancel")
            val answer = JOptionPane.showOptionDialog(null, "Keep your AFK sessions running, or close MGAUTO?", "MGAUTO", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0])
            if (answer >= 0) when (options[answer]) { "Keep running in tray" -> visible = false; "Quit" -> quit() }
        }) {
            LaunchedEffect(Unit) {
                if (args.contains("--smoke-test")) {
                    kotlinx.coroutines.withTimeoutOrNull(60000) { while (!model.state.value.apiReady) kotlinx.coroutines.delay(500) }; kotlinx.coroutines.delay(5000)
                    val screen = Robot().createScreenCapture(Rectangle(window.locationOnScreen, window.size))
                    javax.imageio.ImageIO.write(screen, "png", java.io.File("smoke-desktop.png"))
                    java.io.File("smoke-result.txt").writeText("Desktop window rendered; sessions=" + model.state.value.sessions.size)
                    quit()
                }
            }
            MgAfkTheme {
                MainScreen(model,
                    onLoginRequest = { id ->
                        if (id !in busyBrowsers) scope.launch {
                            busyBrowsers.add(id)
                            try { BrowserHost.open("login", id)?.let { model.setToken(id,it) } }
                            catch(e: Exception) { JOptionPane.showMessageDialog(null,e.message,"Login failed",JOptionPane.ERROR_MESSAGE) }
                            finally { busyBrowsers.remove(id) }
                        }
                    },
                    onPlayRequest = { id, cookie, room, gameUrl ->
                        if (id !in busyBrowsers) scope.launch {
                            busyBrowsers.add(id)
                            val wasConnected = model.state.value.sessions.find { it.id == id }?.connected == true
                            try {
                                if (wasConnected) model.disconnectKeepService(id)
                                BrowserHost.open("play", id, GameLaunch.url(gameUrl, room), GameLaunch.cookie(cookie), model.state.value.settings.injectGeminiMod)
                            } catch(e: Exception) { JOptionPane.showMessageDialog(null,e.message,"Game browser failed",JOptionPane.ERROR_MESSAGE) }
                            finally { busyBrowsers.remove(id); if (wasConnected && model.state.value.sessions.any { it.id == id }) model.connect(id) }
                        }
                    }
                )
            }
        }
    }
    } finally {
        SingleInstanceGuard.release()
    }
}
