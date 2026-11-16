package com.project.bcv.feature.discovery.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.project.bcv.R
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class PassportActivity : AppCompatActivity() {
    private val expanded = linkedSetOf<Long>()
    private var regionQuery = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expanded.addAll(savedInstanceState?.getLongArray("expandedRegions")?.toList().orEmpty())
        regionQuery = savedInstanceState?.getString("regionQuery").orEmpty()
        val model = ViewModelProvider(this)[ExplorerToolsViewModel::class.java]
        val page = ExplorerPage(this, "Hộ chiếu khám phá")
        with(page) {
            val hero = panel(true)
            hero.addView(label("DẤU CHÂN TRÊN MỌI MIỀN", 12, true).apply { setTextColor(color(R.color.white)) })
            val totals = label("Hành trình của bạn", 26, true).apply { setTextColor(color(R.color.white)) }
            hero.line(totals)
            hero.line(label("Mỗi địa danh đã khám phá là một dấu mộc. Hoàn thành một vùng để nhận huy hiệu.", muted = true).apply { setTextColor(0xFFCCE3D9.toInt()) })
            body.addView(hero)
            body.addView(label("Hộ chiếu ghi lại nơi bạn đã khám phá; Bộ sưu tập lưu các thẻ văn hóa bạn nhận được.", muted = true))
            body.line(label("Tiến độ theo vùng", 20, true), 24)
            val searchField = TextInputLayout(this@PassportActivity).apply {
                hint="Tìm vùng miền";boxBackgroundMode=TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(dp(14).toFloat(),dp(14).toFloat(),dp(14).toFloat(),dp(14).toFloat())
            }
            val search = TextInputEditText(this@PassportActivity).apply {
                isSingleLine=true;background=null;setPadding(dp(12),dp(12),dp(12),dp(12));setText(regionQuery)
            }
            searchField.addView(search);body.line(searchField,12)
            val status = label("Đang tải hộ chiếu…", muted = true); body.line(status)
            val retry = button("Thử lại") { model.load("api/passport") }; body.addView(retry); retry.visibility = View.GONE
            val regions = LinearLayout(this@PassportActivity).apply { orientation = LinearLayout.VERTICAL }; body.line(regions,16)
            val noMatch = label("Không tìm thấy vùng phù hợp.",muted=true).apply { visibility=View.GONE };body.addView(noMatch)
            fun filterRegions() {
                var visible=0
                for(i in 0 until regions.childCount) {
                    val card=regions.getChildAt(i)
                    val matches=card.tag.toString().contains(regionQuery.trim(),ignoreCase=true)
                    card.visibility=if(matches) View.VISIBLE else View.GONE
                    if(matches) visible++
                }
                noMatch.visibility=if(regions.childCount>0 && visible==0) View.VISIBLE else View.GONE
            }
            search.doAfterTextChanged { regionQuery=it.toString();filterRegions() }
            model.state.observe(this@PassportActivity) { state ->
                regions.removeAllViews()
                status.visibility = if(state.loading || state.error != null) View.VISIBLE else View.GONE
                status.text = if(state.loading) "Đang tải hộ chiếu…" else state.error
                retry.visibility = if(state.error != null) View.VISIBLE else View.GONE
                state.result?.optJSONArray("regions")?.let { array ->
                    val rows = (0 until array.length()).map { array.getJSONObject(it) }
                    val stamps = rows.sumOf { it.getLong("discovered") }
                    val badges = rows.count { it.getBoolean("hasBadge") }
                    totals.text = "$stamps dấu mộc · $badges huy hiệu"
                    if(rows.isEmpty()) { status.visibility = View.VISIBLE; status.text = "Các vùng khám phá sẽ sớm được cập nhật." }
                    rows.forEach { row ->
                        val id = row.getLong("regionId")
                        val card = panel().apply { tag=row.getString("name") }
                        card.addView(label(row.getString("name"),18,true))
                        card.line(label("${row.getLong("discovered")}/${row.getLong("total")} địa danh · ${row.getInt("progressPercent")}%", muted=true))
                        card.line(progress(row.getInt("progressPercent")),12)
                        if(row.getBoolean("hasBadge")) card.line(label("✓ Đã hoàn thành vùng · Nhận huy hiệu",14,true))
                        val entries = row.getJSONArray("stamps")
                        val details = LinearLayout(this@PassportActivity).apply { orientation = LinearLayout.VERTICAL; visibility = if(id in expanded) View.VISIBLE else View.GONE }
                        for(j in 0 until entries.length()) {
                            val stamp = entries.getJSONObject(j)
                            val date = runCatching { OffsetDateTime.parse(stamp.getString("discoveredAt")).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault("")
                            details.line(button("✓ ${stamp.getString("name")}\n$date") {
                                startActivity(Intent(this@PassportActivity, PlaceDetailActivity::class.java)
                                    .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID,stamp.getLong("placeId"))
                                    .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME,stamp.getString("name"))
                                    .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED,true))
                            })
                        }
                        if(entries.length()>0) {
                            val toggle = button(if(id in expanded) "Thu gọn dấu mộc" else "Xem ${entries.length()} dấu mộc") {}
                            toggle.setOnClickListener {
                                if(!expanded.add(id)) expanded.remove(id)
                                details.visibility = if(id in expanded) View.VISIBLE else View.GONE
                                toggle.text = if(id in expanded) "Thu gọn dấu mộc" else "Xem ${entries.length()} dấu mộc"
                            }
                            card.line(toggle)
                        } else card.line(label(if(row.getLong("total")==0L) "Sắp có địa danh để khám phá" else "Bắt đầu hành trình để ghi dấu đầu tiên.",muted=true))
                        card.addView(details); regions.addView(card)
                    }
                    filterRegions()
                }
            }
        }
        if(model.lastPath==null) model.load("api/passport")
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLongArray("expandedRegions",expanded.toLongArray());outState.putString("regionQuery",regionQuery); super.onSaveInstanceState(outState)
    }
}
