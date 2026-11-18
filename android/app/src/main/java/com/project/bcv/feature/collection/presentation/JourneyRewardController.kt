package com.project.bcv.feature.collection.presentation

import android.content.Intent
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.di.CollectionModule
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.feature.engagement.EngagementUi
import com.project.bcv.feature.engagement.EngagementUi.cardDialog
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.icon
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.popIn
import com.project.bcv.feature.engagement.EngagementUi.primaryButton
import com.project.bcv.feature.engagement.EngagementUi.rounded
import com.project.bcv.feature.engagement.EngagementUi.text
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
    private var dialog: Dialog? = null
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

    /**
     * "Mở khóa địa danh": an open-lock medal pops in, the place's name, then the card it gave
     * (picture, name, collection) once loaded, with "Xem thẻ" and "Tiếp tục khám phá".
     */
    private fun show() {
        val trip = journey ?: return
        if (!active || dialog != null || inbox.dialogShown(trip.id)) return
        lateinit var cardSlot: LinearLayout
        lateinit var open: com.google.android.material.button.MaterialButton
        val current = activity.cardDialog { dlg ->
            val medal = FrameLayout(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(EngagementUi.GOLD_SOFT); setStroke(context.dp(4), EngagementUi.GOLD)
                }
                addView(context.icon(com.project.bcv.R.drawable.ic_m3_lock_open, EngagementUi.TEAL, 44),
                    FrameLayout.LayoutParams(context.dp(44), context.dp(44), Gravity.CENTER))
            }
            addView(medal, LinearLayout.LayoutParams(context.dp(96), context.dp(96)))
            medal.popIn()
            addView(context.text("MỞ KHÓA ĐỊA DANH", 12f, EngagementUi.GOLD, true).apply { letterSpacing = 0.12f },
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = context.dp(14) })
            addView(context.text(trip.destinationName ?: "Điểm đến", 20f, EngagementUi.FOREST, true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(4) })
            addView(context.text("Bạn đã đi hết hành trình. Ảnh, video và câu chuyện của nơi này đã mở.", 14f, EngagementUi.MUTED).apply {
                gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(4) })
            cardSlot = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = rounded(EngagementUi.SOFT, 18, null, context)
                setPadding(context.dp(12), context.dp(12), context.dp(14), context.dp(12))
                addView(context.text("Đang tìm thẻ địa danh…", 14f, EngagementUi.MUTED))
            }
            addView(cardSlot, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(16) })
            open = context.primaryButton("Đang tìm thẻ…").apply { isEnabled = false }
            addView(open, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(16) })
            addView(context.outlinedButton("Tiếp tục khám phá").apply { setOnClickListener { dlg.dismiss() } },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(6) })
        }
        dialog = current
        current.setOnDismissListener {
            if (active) inbox.markDialogShown(trip.id)
            dialog = null
        }
        current.show()
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
                showCard(cardSlot, reward)
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
                cardSlot.removeAllViews()
                cardSlot.addView(activity.text("Chưa tải được thông tin thẻ. Mở Bộ sưu tập để tải lại.", 14f, EngagementUi.MUTED))
                open.text = "Mở bộ sưu tập"
                open.isEnabled = true
                open.setOnClickListener { current.dismiss(); activity.startActivity(Intent(activity, CollectionActivity::class.java)) }
            }
        }
    }

    /** The card row: its picture (or a green card face), "Thẻ mới" / "Thẻ của bạn", name and collection. */
    private fun showCard(slot: LinearLayout, reward: com.project.bcv.feature.collection.domain.NewCollectionCard?) = with(activity) {
        slot.removeAllViews()
        if (reward == null) {
            slot.addView(icon(com.project.bcv.R.drawable.ic_m3_style, EngagementUi.MUTED, 22))
            slot.addView(text("Địa danh này chưa có thẻ trong các bộ sưu tập.", 14f, EngagementUi.MUTED),
                LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
            return@with
        }
        val face = FrameLayout(this).apply {
            background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF2E7D67.toInt(), 0xFF19473B.toInt())).apply { cornerRadius = dp(10).toFloat() }
            clipToOutline = true
            addView(text(reward.card.name.trim().take(1).uppercase(), 24f, 0x55FFFFFF, true).apply { gravity = Gravity.CENTER },
                FrameLayout.LayoutParams(-1, -1))
        }
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        face.addView(image, FrameLayout.LayoutParams(-1, -1))
        reward.card.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            RemoteImages.cached(url, sample = 4)?.let(image::setImageBitmap)
                ?: lifecycleScope.launch { RemoteImages.load(url, sample = 4)?.let(image::setImageBitmap) }
        }
        slot.addView(face, LinearLayout.LayoutParams(dp(52), dp(68)))
        slot.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(if (reward.card.isNew) "THẺ MỚI" else "THẺ CỦA BẠN", 11f,
                if (reward.card.isNew) EngagementUi.GOLD else EngagementUi.MUTED, true).apply { letterSpacing = 0.08f })
            addView(text(reward.card.name, 15f, EngagementUi.FOREST, true).apply { maxLines = 2 })
            addView(text("Bộ sưu tập: ${reward.collectionName}", 12f, EngagementUi.MUTED))
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        slot.popIn(0)
    }
}
