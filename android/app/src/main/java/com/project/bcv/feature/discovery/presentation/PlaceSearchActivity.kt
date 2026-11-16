package com.project.bcv.feature.discovery.presentation

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import java.net.URLEncoder

/** Uses the server's combined filters and paging; all active destinations remain selectable. */
class PlaceSearchActivity : AppCompatActivity() {
    private lateinit var model: ExplorerToolsViewModel
    private lateinit var results: LinearLayout
    private lateinit var status: TextView
    private lateinit var search: Button
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var map: Button
    private lateinit var name: EditText
    private lateinit var minimum: EditText
    private lateinit var maximum: EditText
    private lateinit var days: EditText
    private lateinit var types: Spinner
    private lateinit var interests: Spinner
    private lateinit var sort: Spinner
    private var typeIds = listOf<Long?>(null)
    private var interestIds = listOf<Long?>(null)
    private var page = 1
    private var resultIds = longArrayOf()
    private var appliedFilters = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model=ViewModelProvider(this)[ExplorerToolsViewModel::class.java]
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(0xFFF8F6F0.toInt()) }
        setContentView(root)
        fitExplorerInsets(root)
        fun button(label: String, action: () -> Unit) = Button(this).apply { text=label; setOnClickListener { action() }; root.addView(this) }
        button("‹ Khám phá") { finish() }
        val scroll=ScrollView(this); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val form=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; scroll.addView(form)
        fun input(hintText: String, numeric: Boolean=false)=EditText(this).apply {
            hint=hintText; isSingleLine=true; inputType=if(numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT
            form.addView(this)
        }
        name=input("Tên địa danh")
        minimum=input("Khoảng cách tối thiểu (km)",true)
        maximum=input("Khoảng cách tối đa (km)",true)
        days=input("Số ngày ước tính tối đa",true).apply { inputType=InputType.TYPE_CLASS_NUMBER }
        name.setText(savedInstanceState?.getString("query"))
        minimum.setText(savedInstanceState?.getString("minimum"))
        maximum.setText(savedInstanceState?.getString("maximum"))
        days.setText(savedInstanceState?.getString("days"))
        fun spinner(label: String)=Spinner(this).also { spinner ->
            form.addView(TextView(this).apply { text=label }); form.addView(spinner)
        }
        types=spinner("Loại địa danh / chủ đề")
        interests=spinner("Sở thích — chỉ lọc khi bạn chọn")
        sort=spinner("Sắp xếp").apply { adapter=ArrayAdapter(this@PlaceSearchActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Gần nhất","Tên địa danh","Ít ngày nhất","Ưu tiên sở thích")) }
        sort.setSelection(savedInstanceState?.getInt("sort",0) ?: 0)
        search=Button(this).apply { text="Tìm địa danh"; setOnClickListener {
            val params=mutableListOf("pageSize=20", "sort=${listOf("distance","name","estimatedDays","interests")[sort.selectedItemPosition]}")
            fun add(key:String,value:String) { if(value.isNotBlank()) params.add("$key=${URLEncoder.encode(value.trim(),"UTF-8")}") }
            add("q",name.text.toString()); add("minDistanceKm",minimum.text.toString()); add("maxDistanceKm",maximum.text.toString()); add("maxEstimatedDays",days.text.toString())
            typeIds.getOrNull(types.selectedItemPosition)?.let { add("placeTypeId",it.toString()) }
            interestIds.getOrNull(interests.selectedItemPosition)?.let { add("interestGroupId",it.toString()) }
            appliedFilters=params.joinToString("&"); page=1; load()
        }}; form.addView(search)
        status=TextView(this).apply { setPadding(0,16,0,16) }; form.addView(status)
        val paging=LinearLayout(this); form.addView(paging)
        previous=Button(this).apply { text="Trang trước"; setOnClickListener { page--; load() } }; paging.addView(previous)
        next=Button(this).apply { text="Trang sau"; setOnClickListener { page++; load() } }; paging.addView(next)
        map=Button(this).apply { text="Xem trang kết quả trên bản đồ"; setOnClickListener { setResult(RESULT_OK, Intent().putExtra("mapPlaceIds",resultIds)); finish() } }; form.addView(map)
        results=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; form.addView(results)
        page=savedInstanceState?.getInt("page",1) ?: 1
        appliedFilters=savedInstanceState?.getString("filters") ?: "pageSize=20&sort=distance"
        model.options.observe(this) { json ->
            fun populate(key:String,spinner:Spinner):List<Long?> {
                val array=json.getJSONArray(key); val ids=mutableListOf<Long?>(null); val labels=mutableListOf("Tất cả")
                for(i in 0 until array.length()) { val row=array.getJSONObject(i); ids.add(row.getLong("id")); labels.add(row.getString("name")) }
                spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,labels); return ids
            }
            typeIds=populate("placeTypes",types); interestIds=populate("interests",interests)
            savedInstanceState?.getLong("typeId",0)?.let { id -> types.setSelection(typeIds.indexOf(id).coerceAtLeast(0)) }
            savedInstanceState?.getLong("interestId",0)?.let { id -> interests.setSelection(interestIds.indexOf(id).coerceAtLeast(0)) }
        }
        model.state.observe(this) { state ->
            search.isEnabled=!state.loading; previous.isEnabled=!state.loading && page>1; next.isEnabled=false; map.isEnabled=false
            results.removeAllViews()
            status.text=if(state.loading) "Đang tìm địa danh…" else state.error ?: ""
            state.error?.let { results.addView(Button(this).apply { text="Thử lại"; setOnClickListener { model.loadOptions(); load() } }) }
            state.result?.let { json ->
                val total=json.getLong("total"); val array=json.getJSONArray("places")
                page=json.getInt("page"); status.text=if(total==0L) "Không có địa danh phù hợp. Hãy nới bộ lọc." else "$total địa danh · Trang $page"
                next.isEnabled=page*20<total; map.isEnabled=array.length()>0
                resultIds=LongArray(array.length()) { array.getJSONObject(it).getLong("placeId") }
                for(i in 0 until array.length()) {
                    val row=array.getJSONObject(i)
                    results.addView(Button(this).apply {
                        text="${row.getString("name")}\n${row.getString("regionName")} · ${row.getDouble("distanceKm")} km\n${row.getInt("requiredSteps")} bước · ${row.getInt("estimatedDays")} ngày"
                        isAllCaps=false; contentDescription=text
                        setOnClickListener { startActivity(Intent(this@PlaceSearchActivity,PlaceDetailActivity::class.java).putExtra(PlaceDetailActivity.EXTRA_PLACE_ID,row.getLong("placeId"))) }
                    })
                }
            }
        }
        model.loadOptions()
        if(model.lastPath==null)load()
    }
    private fun load()=model.load("api/explore/places?$appliedFilters&page=$page")
    override fun onSaveInstanceState(outState:Bundle) {
        outState.putInt("page",page);outState.putString("filters",appliedFilters)
        outState.putString("query",name.text.toString());outState.putString("minimum",minimum.text.toString())
        outState.putString("maximum",maximum.text.toString());outState.putString("days",days.text.toString())
        outState.putInt("sort",sort.selectedItemPosition)
        typeIds.getOrNull(types.selectedItemPosition)?.let { outState.putLong("typeId",it) }
        interestIds.getOrNull(interests.selectedItemPosition)?.let { outState.putLong("interestId",it) }
        super.onSaveInstanceState(outState)
    }
}
