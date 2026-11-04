package com.project.bcv.core.network

import android.app.Application
import android.content.Context
import com.project.bcv.BuildConfig

object ApiEnvironment {
    val urls = listOf("https://bcv-admin.onrender.com/", "https://bcv-admin-test.onrender.com/")
    @Volatile var currentUrl: String = BuildConfig.API_BASE_URL
        private set

    fun initialize(context: Context) {
        currentUrl = context.getSharedPreferences("bcv_api_environment", Context.MODE_PRIVATE)
            .getString("url", null)?.takeIf { it in urls } ?: BuildConfig.API_BASE_URL
    }

    fun select(context: Context, url: String) {
        require(url in urls)
        context.getSharedPreferences("bcv_api_environment", Context.MODE_PRIVATE)
            .edit().putString("url", url).apply()
        currentUrl = url
    }
}

class BcvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiEnvironment.initialize(this)
    }
}
