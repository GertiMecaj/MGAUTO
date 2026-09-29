package com.mgafk.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionVersionPolicyTest {

    @Test fun disabled_override_never_forces_cached_value() {
        assertNull(ConnectionVersionPolicy.manualVersion(false, "2026.9.29"))
    }

    @Test fun enabled_override_uses_trimmed_exact_value() {
        assertEquals("2026.9.29", ConnectionVersionPolicy.manualVersion(true, "  2026.9.29  "))
    }

    @Test fun enabled_but_blank_override_is_invalid() {
        assertNull(ConnectionVersionPolicy.manualVersion(true, "   "))
    }
}
