package com.project.bcv.core.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.feature.collection.presentation.CollectionActivity
import com.project.bcv.feature.discovery.domain.ExploreJourney

/** Show a confirmed card receipt once, across tabs, rotations and app restarts. */
object JourneyCompletionDialog {
    fun showIfNeeded(activity: AppCompatActivity, journey: ExploreJourney?) {
        if (journey?.status != "COMPLETED" || activity.isFinishing || activity.isDestroyed) return
        val card = journey.awardedCard ?: return
        val inbox = com.project.bcv.feature.collection.data.CardInbox(activity)
        if (inbox.dialogShown(journey.id)) return
        val preferences = activity.getSharedPreferences("bcv_card_receipts", Context.MODE_PRIVATE)
        val key = "${BuildConfig.API_BASE_URL}:${journey.id}:${card.id}"
        if (preferences.getBoolean(key, false)) return
        MaterialAlertDialogBuilder(activity)
            .setIcon(R.drawable.ic_check)
            .setTitle("Bạn đã nhận được thẻ mới!")
            .setMessage("Đã hoàn thành hành trình đến ${journey.destinationName}.\n\nThẻ “${card.name}” đã có trong bộ sưu tập của bạn.")
            .setPositiveButton("Xem Bộ sưu tập") { _, _ ->
                activity.startActivity(Intent(activity, CollectionActivity::class.java))
            }
            .setNegativeButton("Tiếp tục khám phá", null)
            .show()
        preferences.edit().putBoolean(key, true).apply()
        inbox.recordJourney(journey.id, journey.destinationPlaceId ?: return)
        inbox.markDialogShown(journey.id)
    }
}
