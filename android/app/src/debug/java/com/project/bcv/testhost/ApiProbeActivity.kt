package com.project.bcv.testhost

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.JsonApiClient

/** Debug-only, read-only check using the app's real HTTP/TLS transport and configured endpoint. */
class ApiProbeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply { text = "Đang kiểm tra kết nối API…"; setPadding(32, 100, 32, 32) }
        setContentView(status)
        val client = JsonApiClient(applicationContext, BuildConfig.API_BASE_URL)
        Thread {
            val result = runCatching { client.get("health", "").getString("status") }
            val text = if (result.getOrNull() == "ok") "API đã kết nối\n${BuildConfig.API_BASE_URL}\nstatus=ok"
                else "API chưa kết nối\n${result.exceptionOrNull()?.message.orEmpty()}"
            runOnUiThread { if (!isFinishing && !isDestroyed) status.text = text }
        }.start()
    }
}
