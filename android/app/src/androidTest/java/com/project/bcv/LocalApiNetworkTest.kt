package com.project.bcv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
class LocalApiNetworkTest {
    @Test
    fun emulatorNetworkRoutingDiagnostics() {
        val url = URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/health")
        assumeTrue("This check targets Android Emulator's host alias", url.host == "10.0.2.2")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(ConnectivityManager::class.java)
        fun probe(connection: HttpURLConnection): String {
            val started = System.nanoTime()
            return try {
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                "HTTP ${connection.responseCode} in ${(System.nanoTime() - started) / 1_000_000} ms"
            } catch (error: Exception) {
                error.javaClass.simpleName
            } finally { connection.disconnect() }
        }
        val defaultResult = probe(url.openConnection() as HttpURLConnection)
        Log.i("BCVNetworkProbe", "DEFAULT: $defaultResult")
        val wifi = manager.allNetworks.firstOrNull { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        val wifiResult = wifi?.let { probe(it.openConnection(url) as HttpURLConnection) } ?: "No Wi-Fi"
        Log.i("BCVNetworkProbe", "EXPLICIT_WIFI: $wifiResult")
        org.junit.Assert.assertTrue("Default emulator route failed: $defaultResult", defaultResult.startsWith("HTTP 200"))
    }

    @Test
    fun localApiIsReachableOnWifi() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val response = JsonApiClient(context, BuildConfig.API_BASE_URL).get("health", "")
        assertEquals("ok", response.getString("status"))
    }

    @Test
    fun registrationRequestReachesBackend() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val body = JSONObject()
            .put("email", "not-an-email")
            .put("password", "Testpass123")
            .put("confirmPassword", "Testpass123")
        try {
            JsonApiClient(context, BuildConfig.API_BASE_URL).post("api/account/register", body)
            fail("Backend should reject an invalid email")
        } catch (error: ApiFailure) {
            assertEquals("INVALID_EMAIL", error.code)
        }
    }
}
