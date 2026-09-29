package com.mgafk.app.desktop

import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File
import java.awt.FileDialog
import javax.swing.JOptionPane
import java.util.prefs.Preferences

class DesktopContext private constructor() {
    val filesDir = File(System.getenv("LOCALAPPDATA") ?: (System.getProperty("user.home") + "/.local/share"), "MGAUTO").apply { mkdirs() }
    val applicationContext get() = this
    fun getSharedPreferences(name: String, mode: Int) = DesktopPreferences(name)
    companion object { val instance = DesktopContext(); const val MODE_PRIVATE = 0 }
}
class DesktopPreferences(name: String) {
    private val prefs = Preferences.userRoot().node("MGAUTO/$name")
    fun getString(key: String, fallback: String?): String? = prefs.get(key, fallback)
    fun edit() = this
    fun putString(key: String, value: String): DesktopPreferences { prefs.put(key, value); return this }
    fun apply() = prefs.flush()
}
val LocalContext = staticCompositionLocalOf { DesktopContext.instance }
object DesktopLog {
    fun d(tag: String, message: String) { }
    fun w(tag: String, message: String) { System.err.println("WARN/$tag: $message") }
    fun e(tag: String, message: String, throwable: Throwable? = null) { System.err.println("ERROR/$tag: $message"); throwable?.printStackTrace() }
}
fun parseColor(raw: String): Int {
    val hex = raw.removePrefix("#")
    return when(hex.length) {
        6 -> (0xff000000L or hex.toLong(16)).toInt()
        8 -> hex.toLong(16).toInt()
        3 -> (0xff000000L or hex.flatMap { listOf(it,it) }.joinToString("").toLong(16)).toInt()
        else -> java.awt.Color.decode(raw).rgb
    }
}
fun chooseSound(): String? = FileDialog(null as java.awt.Frame?, "Choose a WAV alarm sound", FileDialog.LOAD).let {
    it.file = "*.wav"; it.isVisible = true
    val result = it.file?.let { name -> File(it.directory, name).absolutePath }; it.dispose(); result
}
fun pickTime(minutes: Int): Int? {
    val text = JOptionPane.showInputDialog(null, "Time (24-hour HH:MM)", "%02d:%02d".format(minutes / 60, minutes % 60)) ?: return null
    val time = runCatching { java.time.LocalTime.parse(text.trim()) }.getOrNull()
    if (time == null) JOptionPane.showMessageDialog(null, "Enter a valid time, for example 22:30.")
    return time?.let { it.hour * 60 + it.minute }
}
class FileExporter(private val callback: (File?) -> Unit) {
    fun launch(name: String) {
        val dialog = FileDialog(null as java.awt.Frame?, "Export log", FileDialog.SAVE)
        dialog.file = name; dialog.isVisible = true
        val file = dialog.file?.let { File(dialog.directory, it) }; dialog.dispose(); callback(file)
    }
}
