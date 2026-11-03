package com.project.bcv.feature.collection.presentation

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.bcv.core.di.CollectionModule
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.statsnotify.notifications.presentation.JourneyCelebrationView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Completion is confirmed by the journey, independently of whether notices were read/deleted. */
class JourneyRewardController(private val activity: AppCompatActivity, private val changed: () -> Unit) : DefaultLifecycleObserver {
    private val inbox = CardInbox(activity)
    private var journey: ExploreJourney? = null
    private var active = false
    private var dialog: AlertDialog? = null
    private var loading: Job? = null

    init { activity.lifecycle.addObserver(this) }

    fun completed(value: ExploreJourney) {
        journey = value
        value.destinationPlaceId?.let {
            if (inbox.recordJourney(value.id, it)) CollectionModule.repository(activity).invalidate()
        }
        show()
    }

    override fun onResume(owner: LifecycleOwner) { active = true; show() }
    override fun onPause(owner: LifecycleOwner) {
        active = false
        loading?.cancel()
        dialog?.dismiss()
        dialog = null
    }

    private fun show() {
        val trip = journey ?: return
        if (!active || dialog != null || inbox.dialogShown(trip.id)) return
        val content = JourneyCelebrationView(activity,
            "Bạn đã hoàn thành hành trình tới ${trip.destinationName}!\n\nĐang tìm thẻ địa danh trong bộ sưu tập…")
        val current = MaterialAlertDialogBuilder(activity).setView(content)
            .setPositiveButton("Tiếp tục khám phá", null)
            .setNeutralButton("Đang tìm thẻ…", null).create()
        dialog = current
        current.setOnDismissListener {
            if (active) inbox.markDialogShown(trip.id)
            dialog = null
        }
        current.show()
        val open = current.getButton(AlertDialog.BUTTON_NEUTRAL)
        open.isEnabled = false
        loading = activity.lifecycleScope.launch {
            try {
                val reward = withContext(Dispatchers.IO) {
                    val repository = CollectionModule.repository(activity)
                    val overview = repository.load()
                    overview.newItems.firstOrNull { it.card.placeId == trip.destinationPlaceId }
                        ?: overview.collections.firstNotNullOfOrNull { collection ->
                            repository.loadDetail(collection.id).cards.firstOrNull {
                                it.owned && it.placeId == trip.destinationPlaceId
                            }?.let { com.project.bcv.feature.collection.domain.NewCollectionCard(it, collection.id, collection.name) }
                        }
                }
                content.setMessage(if (reward != null)
                    "Bạn đã khám phá ${trip.destinationName}!\n\n" +
                        (if (reward.card.isNew) "Đã nhận thẻ “${reward.card.name}”." else "Bạn đã sở hữu thẻ “${reward.card.name}”.") +
                        "\nThuộc bộ sưu tập: ${reward.collectionName}."
                else "Bạn đã hoàn thành hành trình tới ${trip.destinationName}!\n\nĐịa điểm này hiện chưa có thẻ trong các bộ sưu tập.")
                open.text = if (reward?.card?.isNew == true) "Xem thẻ vừa nhận" else if (reward != null) "Xem thẻ địa danh" else "Mở bộ sưu tập"
                open.isEnabled = true
                open.setOnClickListener {
                    current.dismiss()
                    activity.startActivity(if (reward != null) CardIntents.detail(activity, reward)
                        else Intent(activity, CollectionActivity::class.java))
                }
                changed()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                content.setMessage("Bạn đã hoàn thành hành trình tới ${trip.destinationName}!\n\nChưa tải được thông tin thẻ. Mở Bộ sưu tập để tải lại.")
                open.text = "Mở bộ sưu tập"
                open.isEnabled = true
                open.setOnClickListener { current.dismiss(); activity.startActivity(Intent(activity, CollectionActivity::class.java)) }
            }
        }
    }
}
