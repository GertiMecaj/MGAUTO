package com.mgafk.app.data.repository

/**
 * Resolves the explicit game-version override without conflating it with Session.gameVersion,
 * which stores the effective version used by the current/most recent connection.
 */
object ConnectionVersionPolicy {
    fun manualVersion(enabled: Boolean, value: String): String? {
        if (!enabled) return null
        return value.trim().takeIf { it.isNotEmpty() }
    }
}
