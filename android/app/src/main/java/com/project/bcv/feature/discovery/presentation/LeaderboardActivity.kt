package com.project.bcv.feature.discovery.presentation

import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.project.bcv.R
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

class LeaderboardActivity : AppCompatActivity() {
    private var weekOffset = 0L
    private var regionId: Long? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weekOffset = savedInstanceState?.getLong("weekOffset") ?: 0
        regionId = savedInstanceState?.getLong("regionId",0)?.takeIf { it>0 }
        val model = ViewModelProvider(this)[ExplorerToolsViewModel::class.java]
        val page = ExplorerPage(this,"Bảng xếp hạng")
        with(page) {
            val hero = panel(true)
            hero.addView(label("CÙNG NHAU KHÁM PHÁ",12,true).apply { setTextColor(color(R.color.white)) })
            hero.line(label("Nhà khám phá tuần",26,true).apply { setTextColor(color(R.color.white)) })
            hero.line(label("Mỗi hành trình là một bước tiến. Khám phá, tích điểm và theo dõi thành tích mỗi tuần.").apply { setTextColor(0xFFCCE3D9.toInt()) })
            body.addView(hero)
            val controls = panel()
            val period = label("Tuần hiện tại",16,true); controls.addView(period)
            val weekButtons = LinearLayout(this@LeaderboardActivity)
            val previous = button("‹ Tuần trước") {}; val current = button("Tuần này") {}; val next = button("Tuần sau ›") {}
            listOf(previous,current,next).forEachIndexed { index, button ->
                button.textSize=12f; button.setPadding(dp(4),0,dp(4),0); button.minWidth=0
                weekButtons.addView(button,LinearLayout.LayoutParams(0,dp(60),1f).apply { if(index>0) marginStart=dp(6) })
            }
            controls.line(weekButtons)
            val field = TextInputLayout(this@LeaderboardActivity).apply {
                hint = "Phạm vi xếp hạng"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(dp(14).toFloat(),dp(14).toFloat(),dp(14).toFloat(),dp(14).toFloat())
            }
            val regions = MaterialAutoCompleteTextView(this@LeaderboardActivity).apply {
                inputType = 0; background=null; setPadding(dp(12),dp(12),dp(12),dp(12))
                setText("Toàn quốc",false); setTextColor(color(R.color.forest))
            }
            field.addView(regions); controls.line(field,12); body.addView(controls)
            val mePanel = panel(true)
            mePanel.addView(label("VỊ TRÍ CỦA BẠN",12,true).apply { setTextColor(color(R.color.white));isAccessibilityHeading=true })
            val meText = label("Đang tải…",20,true).apply { setTextColor(color(R.color.white)) }; mePanel.line(meText)
            val meContext=label("",13).apply { setTextColor(0xFFCCE3D9.toInt()) };mePanel.line(meContext)
            body.addView(mePanel,1)
            body.line(label("Những bước chân nổi bật",20,true),8)
            val status = label("Đang tải xếp hạng…",muted=true); body.line(status)
            val list = LinearLayout(this@LeaderboardActivity).apply { orientation=LinearLayout.VERTICAL }; body.line(list,16)
            fun load() {
                val date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusWeeks(weekOffset)
                val iso = WeekFields.ISO
                val key = "%04d-W%02d".format(date.get(iso.weekBasedYear()),date.get(iso.weekOfWeekBasedYear()))
                model.load("api/leaderboard?limit=20&week=$key" + (regionId?.let { "&regionId=$it" } ?: ""))
                next.isEnabled = weekOffset < 0
            }
            val retry = button("Thử lại") { load() }; body.addView(retry); retry.visibility=View.GONE
            previous.setOnClickListener { weekOffset--;load() }
            current.setOnClickListener { weekOffset=0;load() }
            next.setOnClickListener { if(weekOffset<0) { weekOffset++;load() } }
            model.options.observe(this@LeaderboardActivity) { json ->
                val array=json.getJSONArray("regions")
                val ids=mutableListOf<Long?>(null); val names=mutableListOf("Toàn quốc")
                for(i in 0 until array.length()) { val row=array.getJSONObject(i);ids.add(row.getLong("id"));names.add(row.getString("name")) }
                regions.setAdapter(ArrayAdapter(this@LeaderboardActivity,android.R.layout.simple_dropdown_item_1line,names))
                regions.setText(names[ids.indexOf(regionId).coerceAtLeast(0)],false)
                regions.setOnItemClickListener { _,_,position,_ -> regionId=ids[position];load() }
            }
            val rulesNote=label("Điểm được ghi nhận từ hoạt động hợp lệ.",muted=true)
            model.state.observe(this@LeaderboardActivity) { state ->
                list.removeAllViews()
                status.visibility=if(state.loading || state.error!=null) View.VISIBLE else View.GONE
                status.text=if(state.loading) "Đang tải xếp hạng…" else state.error
                retry.visibility=if(state.error!=null) View.VISIBLE else View.GONE
                mePanel.visibility=if(state.result!=null) View.VISIBLE else View.GONE
                state.result?.let { json ->
                    json.optJSONObject("rewardRules")?.let { rules ->
                        rulesNote.text="Khám phá địa danh mới bằng hành trình đi bộ: +${rules.getInt("discoveryPoints")} điểm, một lần cho mỗi địa danh. Điểm nhiệm vụ được cấu hình riêng, xem tại mục Nhiệm vụ. Vé bay không cộng điểm khám phá. Điểm cũ được giữ nguyên. Xếp hạng theo tuần giờ Việt Nam."
                    }
                    val format=DateTimeFormatter.ofPattern("dd/MM")
                    val zone=ZoneId.of("Asia/Ho_Chi_Minh")
                    val start=OffsetDateTime.parse(json.getString("startsAt")).atZoneSameInstant(zone).format(format)
                    val end=OffsetDateTime.parse(json.getString("endsAt")).atZoneSameInstant(zone).minusDays(1).format(format)
                    period.text="${if(weekOffset==0L) "Tuần này" else "Tuần đã chọn"} · $start – $end"
                    val me=json.getJSONObject("me")
                    meText.text="Bạn: ${if(me.isNull("rank")) "Chưa xếp hạng" else "Hạng ${me.getLong("rank")}"} · ${me.getLong("points")} điểm"
                    meContext.text="${regions.text} · $start – $end" + if(me.isNull("rank")) "\nHoàn thành nhiệm vụ hoặc hành trình đi bộ hợp lệ để bắt đầu ghi điểm." else "\nDòng của bạn được đánh dấu trong danh sách."
                    mePanel.contentDescription="Vị trí của bạn. ${meText.text}. ${meContext.text}"
                    val top=json.getJSONArray("top")
                    if(top.length()==0) {
                        val empty=panel();empty.addView(label("Chưa có thành tích tuần này",18,true))
                        empty.line(label("Hoàn thành hành trình đi bộ để bắt đầu ghi điểm. Bạn cũng có thể xem tuần trước hoặc chọn vùng khác.",muted=true));list.addView(empty)
                    }
                    val rows=(0 until top.length()).map { top.getJSONObject(it) }.toMutableList()
                    if(!me.isNull("rank") && rows.none { it.getLong("userId")==me.getLong("userId") }) {
                        rows.add(me)
                    }
                    for(row in rows) {
                        val rank=row.getLong("rank")
                        val isMe=row.getLong("userId")==me.getLong("userId")
                        val card=panel(isMe); val line=LinearLayout(this@LeaderboardActivity).apply { gravity=Gravity.CENTER_VERTICAL }
                        line.addView(label("#${rank}",22,true).apply { setTextColor(if(rank==1L) 0xFFA0712B.toInt() else color(R.color.teal)) },LinearLayout.LayoutParams(dp(56),-2))
                        line.addView(label(if(isMe) "Bạn · ${row.getString("displayName")}" else row.getString("displayName"),15,true),LinearLayout.LayoutParams(0,-2,1f))
                        line.addView(label("${row.getLong("points")}\nđiểm",16,true).apply { gravity=Gravity.END })
                        if(isMe) for(i in 0 until line.childCount) (line.getChildAt(i) as TextView).setTextColor(color(R.color.white))
                        card.addView(line);list.addView(card)
                    }
                }
            }
            body.line(rulesNote,16)
            model.loadOptions()
            if(model.lastPath==null) load()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("weekOffset",weekOffset);outState.putLong("regionId",regionId?:0);super.onSaveInstanceState(outState)
    }
}
