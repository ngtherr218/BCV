package com.project.bcv.feature.engagement

import com.project.bcv.feature.engagement.data.EngagementApi
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.ui.NavTab
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.scaffold
import com.project.bcv.feature.engagement.EngagementUi.text
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Bộ sưu tập › Hộ chiếu (F81): places stamped by real GPS check-ins, grouped into the 34 provinces. */
class PassportActivity : AppCompatActivity() {
    private lateinit var body: LinearLayout
    private var shown: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = scaffold("Bộ sưu tập", tab = NavTab.COLLECTION, top = EngagementBlocks.collectionSwitcher(this, "passport"))
        // The last passport is drawn at once, so switching from Thẻ never shows an empty page first.
        cached?.let(::render)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        if (body.childCount == 0) body.full(text("Đang tải hộ chiếu…", 15f, MUTED), 16)
        lifecycleScope.launch {
            val result = runCatching { EngagementApi(this@PassportActivity).summary().getJSONObject("passport") }
            result.onSuccess { passport -> cached = passport; render(passport) }
                .onFailure {
                    if (shown != null) return@onFailure
                    body.removeAllViews()
                    body.full(text(it.message ?: "Không tải được hộ chiếu.", 15f, MUTED), 16)
                    body.full(outlinedButton("Thử lại").apply { setOnClickListener { load() } }, 12)
                }
        }
    }

    /** Redraws only when the data changed, keeping the scroll position. */
    private fun render(passport: JSONObject) {
        val json = passport.toString()
        if (json == shown) return
        shown = json
        body.removeAllViews()
        EngagementBlocks.passport(this, body, passport)
    }

    companion object {
        private var cached: JSONObject? = null
    }
}
