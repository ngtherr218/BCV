package com.project.bcv

import android.app.Application
import com.project.bcv.core.ui.RemoteImages

class BcvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RemoteImages.initialize(this)
    }
}
