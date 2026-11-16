package com.project.bcv.feature.discovery.presentation

import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

internal fun AppCompatActivity.fitExplorerInsets(view: View) {
    enableEdgeToEdge()
    val padding=(20*resources.displayMetrics.density).toInt()
    ViewCompat.setOnApplyWindowInsetsListener(view) { target,insets ->
        val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
        target.setPadding(padding+bars.left,padding+bars.top,padding+bars.right,padding+bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(view)
}
