package com.project.bcv.feature.engagement

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.ui.NavTab
import com.project.bcv.feature.engagement.EngagementUi.FOREST
import com.project.bcv.feature.engagement.EngagementUi.GOLD_SOFT
import com.project.bcv.feature.engagement.EngagementUi.MUTED
import com.project.bcv.feature.engagement.EngagementUi.card
import com.project.bcv.feature.engagement.EngagementUi.dp
import com.project.bcv.feature.engagement.EngagementUi.full
import com.project.bcv.feature.engagement.EngagementUi.text
import com.project.bcv.feature.engagement.EngagementUi.scaffold
import com.project.bcv.feature.engagement.EngagementUi.outlinedButton
import com.project.bcv.feature.engagement.EngagementUi.primaryButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** BCV-ther ranking presentation, using the v2 score ledger and weekly ranking API. */
class AchievementsActivity : AppCompatActivity() {
    private lateinit var body: LinearLayout
    private var request: Job? = null
    private var previousWeek = false
    private val points by lazy { text("— điểm", 20f, FOREST, true) }
    private val caption by lazy { text("", 13f, MUTED) }
    private val tabs by lazy { FrameLayout(this) }
    private val stage by lazy { FrameLayout(this) }
    private val rows by lazy { LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previousWeek = savedInstanceState?.getBoolean("previousWeek") ?: false
        body = scaffold("Xếp hạng", "Mỗi bước chân, một dấu ấn", tab = NavTab.RANKING)
        body.full(card(color = GOLD_SOFT).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(text("Điểm khám phá của bạn", 13f, MUTED), LinearLayout.LayoutParams(0,-2,1f))
            addView(points)
        }, 4)
        body.full(tabs,16); body.full(caption,12)
        body.addView(stage,LinearLayout.LayoutParams(-1,dp(270)).apply { topMargin=dp(8) })
        body.full(rows,12)
        body.full(outlinedButton("Chọn tuần và khu vực") .apply {
            setOnClickListener { startActivity(Intent(this@AchievementsActivity,com.project.bcv.feature.discovery.presentation.LeaderboardActivity::class.java)) }
        },16)
        body.full(text("Điểm từ hành trình, câu hỏi và nhiệm vụ được ghi nhận theo quy tắc của BCV v2. Xếp hạng chốt theo tuần giờ Việt Nam.",13f,MUTED),12)
    }
    override fun onResume() { super.onResume(); load() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBoolean("previousWeek",previousWeek); super.onSaveInstanceState(outState) }
    private fun load() {
        request?.cancel()
        tabs.removeAllViews()
        tabs.addView(LeaderboardViews.segmented(this,if(previousWeek) "previous" else "current",listOf("current" to "Tuần này","previous" to "Tuần trước")) {
            previousWeek=it=="previous";load()
        })
        stage.removeAllViews();stage.addView(LeaderboardViews.podium(this,lifecycleScope,null,loading=true));rows.removeAllViews()
        request=lifecycleScope.launch {
            try {
                val date=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusWeeks(if(previousWeek) 1 else 0)
                val iso=java.time.temporal.WeekFields.ISO
                val week="%04d-W%02d".format(date.get(iso.weekBasedYear()),date.get(iso.weekOfWeekBasedYear()))
                val board=EngagementApi(this@AchievementsActivity).ranking(week)
                val me=board.getJSONObject("me")
                points.text="${me.getLong("points")} điểm"
                caption.text="Toàn quốc · $week · "+if(me.isNull("rank")) "Bạn chưa có thứ hạng" else "Bạn đang ở hạng ${me.getLong("rank")}"
                fun adapt(row: JSONObject)=JSONObject().put("name",row.getString("displayName"))
                    .put("value",row.getLong("points")).put("unit","điểm").put("rank",row.optLong("rank"))
                    .put("isMe",row.getLong("userId")==me.getLong("userId"))
                val top=board.getJSONArray("top");val display=JSONArray()
                for(i in 0 until top.length()) display.put(adapt(top.getJSONObject(i)))
                stage.removeAllViews();stage.addView(LeaderboardViews.podium(this@AchievementsActivity,lifecycleScope,display))
                for(i in 3 until display.length()) rows.full(LeaderboardViews.rankRow(this@AchievementsActivity,lifecycleScope,display.getJSONObject(i)),8)
                if(!me.isNull("rank") && (0 until top.length()).none { top.getJSONObject(it).getLong("userId")==me.getLong("userId") })
                    rows.full(LeaderboardViews.rankRow(this@AchievementsActivity,lifecycleScope,adapt(me)),12)
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch(error: Exception) {
                stage.removeAllViews();stage.addView(LeaderboardViews.podium(this@AchievementsActivity,lifecycleScope,null))
                rows.full(text(error.message ?: "Không tải được bảng xếp hạng.",14f,MUTED))
                rows.full(primaryButton("Thử lại").apply { setOnClickListener { load() } },12)
            }
        }
    }
}
