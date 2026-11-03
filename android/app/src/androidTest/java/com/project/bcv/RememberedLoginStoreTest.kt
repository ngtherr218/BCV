package com.project.bcv

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.bcv.feature.account.auth.data.local.RememberedLoginStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RememberedLoginStoreTest {
    @Test fun encryptsAndRestoresCredentialsAndClearsThem() = fixture { store, prefs ->
        val email = "remember-test@example.com"
        val password = "Test-only-password-123!"
        store.save(email, password)
        assertEquals(email to password, store.load())
        val disk = prefs.getString("credentials", "").orEmpty()
        assertFalse(disk.contains(email))
        assertFalse(disk.contains(password))
        store.clear()
        assertNull(store.load())
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun rejectsCorruptedCredentialsWithoutCrashing() = fixture { store, prefs ->
        store.save("test@example.com", "test-password")
        prefs.edit().putString("credentials", "corrupted").commit()
        assertNull(store.load())
        assertTrue(prefs.all.isEmpty())
    }

    private fun fixture(block: (RememberedLoginStore, android.content.SharedPreferences) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = "remember-login-test-${UUID.randomUUID()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(scope.toByteArray()).joinToString("") { "%02x".format(it) }
        val name = "bcv_login_$hash"
        val prefs = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
        try { block(RememberedLoginStore(context, scope), prefs) }
        finally {
            prefs.edit().clear().commit()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(name) }
        }
    }
}
