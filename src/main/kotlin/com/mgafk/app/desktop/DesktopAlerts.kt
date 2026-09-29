package com.mgafk.app.desktop

import java.awt.TrayIcon
import java.io.File
import javax.sound.sampled.*
import javax.swing.*

object DesktopAlerts {
    var tray: TrayIcon? = null
    private var clip: Clip? = null
    private var dialog: JDialog? = null
    fun show(title: String, body: String, alarm: Boolean = false, sound: String = "", volume: Float = 1f) {
        SwingUtilities.invokeLater {
            tray?.displayMessage(title, body, TrayIcon.MessageType.INFO)
            if (alarm) {
                stop()
                preview(sound, volume, true)
                val pane = JOptionPane(body, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null, arrayOf("Stop alarm"))
                dialog = pane.createDialog(null, title).apply {
                    isModal = false; isAlwaysOnTop = true
                    addWindowListener(object : java.awt.event.WindowAdapter() { override fun windowClosing(e: java.awt.event.WindowEvent) { stop() } })
                    pane.addPropertyChangeListener { if (it.propertyName == JOptionPane.VALUE_PROPERTY) stop() }
                    isVisible = true
                }
            }
        }
    }
    fun preview(sound: String, volume: Float, loop: Boolean = false) {
        clip?.close()
        runCatching {
            val stream = if (sound.isNotBlank()) AudioSystem.getAudioInputStream(File(sound)) else {
                val rate = 22050f
                val data = ByteArray(22050 * 2)
                for (i in 0 until 22050) {
                    val sample = if (i % 11025 < 7000) (kotlin.math.sin(2 * Math.PI * 880 * i / rate) * 12000).toInt() else 0
                    data[i * 2] = sample.toByte(); data[i * 2 + 1] = (sample shr 8).toByte()
                }
                AudioInputStream(data.inputStream(), AudioFormat(rate, 16, 1, true, false), 22050)
            }
            clip = AudioSystem.getClip().apply {
                stream.use { open(it) }
                if (isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    gain.value = (20 * kotlin.math.log10(volume.coerceAtLeast(0.0001f))).coerceIn(gain.minimum, gain.maximum)
                }
                if (loop) loop(Clip.LOOP_CONTINUOUSLY) else start()
            }
        }.onFailure { java.awt.Toolkit.getDefaultToolkit().beep() }
    }
    fun stop() { clip?.stop(); clip?.close(); clip = null; val old = dialog; dialog = null; old?.dispose() }
}
