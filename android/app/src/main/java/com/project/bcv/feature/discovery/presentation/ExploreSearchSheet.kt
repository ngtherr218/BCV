package com.project.bcv.feature.discovery.presentation

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.project.bcv.R
import com.project.bcv.feature.discovery.domain.ExploreMapPlace
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.PlaceSearch
import java.util.Locale

/**
 * Search sheet for the Explore map. Users rarely know exact names, so it leads with suggestions:
 * place-type chips (with counts) and, before anything is typed, the places nearest the virtual location.
 * Typing filters the map behind the sheet live; tapping a result opens that place.
 */
class ExploreSearchSheet(
    private val activity: AppCompatActivity,
    private val snapshot: ExploreSnapshot,
    initialQuery: String,
    initialFilters: SearchFilters,
    private val dailyGoal: Int,
    private val onQuery: (String, SearchFilters) -> Unit,
    private val onPlace: (ExploreMapPlace) -> Unit,
) {
    private var activeFilters = initialFilters
    private val filterChips by lazy { root.findViewById<ChipGroup>(R.id.filterChips) }
    private val dialog = BottomSheetDialog(activity)
    private val root = LayoutInflater.from(activity).inflate(R.layout.dialog_explore_search, null)
    private val input = root.findViewById<android.widget.EditText>(R.id.searchInput)
    private val clear = root.findViewById<View>(R.id.searchClear)
    private val chips = root.findViewById<ChipGroup>(R.id.typeChips)
    private val label = root.findViewById<TextView>(R.id.resultLabel)
    private val empty = root.findViewById<View>(R.id.resultEmpty)
    private val showOnMap = root.findViewById<MaterialButton>(R.id.showOnMap)
    private val list = root.findViewById<RecyclerView>(R.id.resultList)
    private val adapter = ResultAdapter()
    private val origin = snapshot.location?.coordinates
    private val distances = snapshot.places.associate { place ->
        place.id to (origin?.let { PlaceSearch.distanceKm(it.latitude, it.longitude, place.coordinates.latitude, place.coordinates.longitude) })
    }
    private var syncingChips = false

    init {
        dialog.setContentView(root)
        root.findViewById<View>(R.id.searchClose).setOnClickListener { dialog.dismiss() }
        list.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = this@ExploreSearchSheet.adapter
        }
        buildChips()
        buildFilterChips()
        input.setText(initialQuery)
        input.setSelection(input.text.length)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refresh(notify = true)
        })
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { dialog.dismiss(); true } else false
        }
        clear.setOnClickListener { input.setText("") }
        showOnMap.setOnClickListener { dialog.dismiss() }
        refresh(notify = false)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        // Keyboard only when the user is refining an existing query; otherwise lead with the suggestions.
        dialog.window?.setSoftInputMode(
            if (initialQuery.isNotEmpty()) WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            else WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        if (initialQuery.isNotEmpty()) input.requestFocus()
    }

    fun show() {
        dialog.show()
        // Material3 tints the sheet with a lavender surface colour; keep it white like the rest of the app.
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.backgroundTintList =
            ColorStateList.valueOf(0xFFFFFFFF.toInt())
    }
    fun setOnDismiss(action: () -> Unit) = dialog.setOnDismissListener { action() }

    private fun buildChips() {
        val counts = snapshot.places.filter { it.placeType.isNotBlank() }.groupingBy { it.placeType }.eachCount()
        // Older servers don't send place types on the map list; hide the row instead of showing an empty label.
        if (counts.isEmpty()) {
            root.findViewById<View>(R.id.typeLabel).visibility = View.GONE
            (chips.parent as View).visibility = View.GONE
            return
        }
        val teal = ContextCompat.getColor(activity, R.color.teal)
        counts.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            chips.addView(Chip(activity).apply {
                text = "$type · $count"
                tag = type
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(teal, 0xFFF1F5F1.toInt()))
                setTextColor(ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(0xFFFFFFFF.toInt(), ContextCompat.getColor(activity, R.color.forest))))
                chipStrokeWidth = 0f
                setOnClickListener {
                    if (syncingChips) return@setOnClickListener
                    input.setText(if (isChecked) type else "")
                    input.setSelection(input.text.length)
                }
            })
        }
    }

    /** Three single-choice groups in one row; tapping the selected chip again clears that group. */
    private fun buildFilterChips() {
        val teal = ContextCompat.getColor(activity, R.color.teal)
        val groups = listOf(SearchFilters.DISTANCE, SearchFilters.DURATION, SearchFilters.STATUS)
        groups.forEachIndexed { group, labels ->
            labels.forEachIndexed { index, text ->
                filterChips.addView(Chip(activity).apply {
                    this.text = text
                    tag = group to index + 1
                    isCheckable = true
                    isCheckedIconVisible = false
                    chipBackgroundColor = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(teal, 0xFFFFFFFF.toInt()))
                    setTextColor(ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(0xFFFFFFFF.toInt(), ContextCompat.getColor(activity, R.color.forest))))
                    chipStrokeWidth = activity.resources.displayMetrics.density
                    chipStrokeColor = ColorStateList.valueOf(0xFFDCE7DE.toInt())
                    setOnClickListener {
                        val value = if (isChecked) index + 1 else 0
                        activeFilters = when (group) {
                            0 -> activeFilters.copy(distance = value)
                            1 -> activeFilters.copy(duration = value)
                            else -> activeFilters.copy(status = value)
                        }
                        syncFilterChips()
                        refresh(notify = true)
                    }
                })
            }
        }
        syncFilterChips()
    }

    private fun syncFilterChips() {
        for (i in 0 until filterChips.childCount) {
            val chip = filterChips.getChildAt(i) as Chip
            @Suppress("UNCHECKED_CAST") val pair = chip.tag as Pair<Int, Int>; val (group, value) = pair
            chip.isChecked = value == when (group) { 0 -> activeFilters.distance; 1 -> activeFilters.duration; else -> activeFilters.status }
        }
    }

    private fun refresh(notify: Boolean) {
        val query = input.text.toString().trim()
        clear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        syncingChips = true
        for (i in 0 until chips.childCount) {
            val chip = chips.getChildAt(i) as Chip
            chip.isChecked = query.equals(chip.tag as String, ignoreCase = true)
        }
        syncingChips = false

        val results = rank(query).filter { activeFilters.matches(it, distances[it.id], dailyGoal) }
        val narrowed = query.isNotEmpty() || activeFilters.active
        adapter.submit(if (!narrowed) results.take(30) else results)
        empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        label.text = when {
            !narrowed && origin != null -> "GẦN VỊ TRÍ ẢO CỦA BẠN"
            !narrowed -> "TẤT CẢ ĐỊA DANH"
            else -> "${results.size} KẾT QUẢ"
        }
        showOnMap.text = if (!narrowed) "Xem tất cả trên bản đồ" else "Xem ${results.size} địa danh trên bản đồ"
        showOnMap.isEnabled = !narrowed || results.isNotEmpty()
        if (notify) onQuery(query, activeFilters)
    }

    /** Name prefix first, then name contains, then type match; nearest first within each group. */
    private fun rank(query: String): List<ExploreMapPlace> {
        val byDistance = compareBy<ExploreMapPlace> { distances[it.id] ?: Double.MAX_VALUE }.thenBy { it.name }
        if (query.isEmpty()) return snapshot.places.sortedWith(byDistance)
        val q = PlaceSearch.normalize(query)
        return PlaceSearch.filter(snapshot.places, query).sortedWith(compareBy<ExploreMapPlace> {
            val name = PlaceSearch.normalize(it.name)
            when {
                name.startsWith(q) -> 0
                name.split(' ').any { word -> word.startsWith(q) } -> 1
                name.contains(q) -> 2
                else -> 3
            }
        }.then(byDistance))
    }

    private fun meta(place: ExploreMapPlace): String = listOfNotNull(
        place.placeType.takeIf(String::isNotBlank),
        distances[place.id]?.let { km ->
            (if (km < 1) "${(km * 1000).toInt()} m" else String.format(Locale("vi", "VN"), "%.1f km", km)) +
                " · ~${SearchFilters.days(km, dailyGoal)} ngày đi"
        }
    ).joinToString(" · ")

    private inner class ResultAdapter : RecyclerView.Adapter<ResultAdapter.Holder>() {
        private var items = emptyList<ExploreMapPlace>()

        fun submit(value: List<ExploreMapPlace>) {
            items = value
            @Suppress("NotifyDataSetChanged") notifyDataSetChanged()
            list.scrollToPosition(0)
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.resultName)
            val meta: TextView = view.findViewById(R.id.resultMeta)
            val badge: View = view.findViewById(R.id.resultBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_explore_search_result, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val place = items[position]
            holder.name.text = place.name
            holder.meta.text = meta(place)
            holder.badge.visibility = if (place.isDiscovered) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                dialog.dismiss()
                onPlace(place)
            }
        }
    }

    private companion object {
    }
}
