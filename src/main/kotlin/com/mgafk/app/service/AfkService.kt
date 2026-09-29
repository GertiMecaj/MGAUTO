package com.mgafk.app.service

import com.mgafk.app.data.model.AppSettings
import com.mgafk.app.data.model.WakeLockMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.sun.jna.platform.win32.Kernel32

object AfkService {
    const val MODE_OFF = 0
    const val MODE_SMART = 1
    const val MODE_ALWAYS = 2
    data class ServiceLog(val timestamp: Long = System.currentTimeMillis(), val event: String, val detail: String)
    private val events = MutableSharedFlow<ServiceLog>(extraBufferCapacity = 64)
    val logs = events.asSharedFlow()
    @Volatile private var keepAwake = false
    private val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "MGAUTO-power").apply { isDaemon = true } }
    init {
        executor.scheduleAtFixedRate({
            if (System.getProperty("os.name").startsWith("Windows")) {
                runCatching { Kernel32.INSTANCE.SetThreadExecutionState(if (keepAwake) 0x80000001.toInt() else 0x80000000.toInt()) }
                    .onFailure { log("Power", it.message.orEmpty()) }
            }
        }, 0, 15, TimeUnit.SECONDS)
    }
    fun configure(settings: AppSettings) { keepAwake = settings.wakeLockMode != WakeLockMode.OFF; log("Background", if (keepAwake) "Preventing system sleep while active" else "Using Windows sleep settings") }
    fun stop() { keepAwake = false; executor.execute { if (System.getProperty("os.name").startsWith("Windows")) runCatching { Kernel32.INSTANCE.SetThreadExecutionState(0x80000000.toInt()) } } }
    fun log(event: String, detail: String) { events.tryEmit(ServiceLog(event = event, detail = detail)) }
}
