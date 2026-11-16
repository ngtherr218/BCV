package com.project.bcv.feature.discovery.presentation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.project.bcv.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.project.bcv.core.ui.confirmReplaceJourney
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.core.di.ExploreModule
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.core.ui.setUpDetailScreen
import com.project.bcv.core.ui.showContent
import com.project.bcv.core.ui.showError
import com.project.bcv.databinding.ActivityRecommendationBinding
import com.project.bcv.databinding.ItemRecommendationBinding
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.discovery.domain.RecommendedPlace
import com.project.bcv.feature.discovery.domain.Recommendations
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/** "Gợi ý điểm đến": the server's top three destinations from the virtual location. */
class RecommendationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecommendationBinding
    private lateinit var model: RecommendationViewModel
    private val cards = mutableMapOf<Long, ItemRecommendationBinding>()
    private val vietnamese = Locale.forLanguageTag("vi-VN")
    private lateinit var searchModel: ExplorerToolsViewModel
    private lateinit var searchInput: EditText
    private lateinit var distanceInput: EditText
    private lateinit var daysInput: EditText
    private lateinit var preferInterests: CheckBox
    private lateinit var typeChips: ChipGroup
    private lateinit var previousPage: Button
    private lateinit var nextPage: Button
    private lateinit var retrySearch: Button
    private lateinit var showMap: Button
    private lateinit var filterDialog: BottomSheetDialog
    private lateinit var filterToggle: Button
    private var shownPlaceIds = longArrayOf()
    private val selectedTypes = linkedSetOf<Long>()
    private val profileTypes = linkedSetOf<Long>()
    private var preferencesInitialized = false
    private lateinit var filterSheet: com.project.bcv.databinding.SheetDestinationFiltersBinding
    private var usingSearch = false
    private var filterPage = 1
    private var suppressChanges = false
    private var debounce: Job? = null
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecommendationBinding.inflate(layoutInflater)
        // Like the other screens: content padded clear of the status and navigation bars.
        setUpDetailScreen(binding.root)
        model = ViewModelProvider(this, viewModelFactory {
            RecommendationViewModel(ExploreModule.repository(this), ExploreModule.addressLookup(),
                intent.getStringExtra(EXTRA_ADDRESS))
        })[RecommendationViewModel::class.java]
        searchModel = ViewModelProvider(this)[ExplorerToolsViewModel::class.java]
        buildFilters(savedInstanceState)
        searchModel.options.observe(this) { options ->
            suppressChanges = true
            val defaults = options.getJSONArray("selectedPlaceTypeIds")
            profileTypes.clear()
            for (i in 0 until defaults.length()) profileTypes.add(defaults.getLong(i))
            val initialize = !preferencesInitialized
            if (initialize) { selectedTypes.addAll(profileTypes); preferencesInitialized = true }
            typeChips.removeAllViews()
            val types = options.getJSONArray("interests")
            for (index in 0 until types.length()) {
                val row = types.getJSONObject(index)
                val id = row.getLong("id")
                typeChips.addView(Chip(this).apply {
                    text = row.getString("name"); isCheckable = true; isChecked = id in selectedTypes
                    tag = id; isCheckedIconVisible = true
                    setCheckedIconResource(R.drawable.ic_check)
                    chipBackgroundColor = getColorStateList(R.color.destination_chip_background)
                    setTextColor(getColorStateList(R.color.destination_chip_text))
                    checkedIconTint = ColorStateList.valueOf(getColor(R.color.white))
                    chipStrokeColor = ColorStateList.valueOf(getColor(R.color.field_line))
                    chipStrokeWidth = dp(1).toFloat(); chipCornerRadius = dp(12).toFloat()
                    chipMinHeight = dp(44).toFloat(); textSize = 13f
                    setOnCheckedChangeListener { _, checked ->
                        if (suppressChanges) return@setOnCheckedChangeListener
                        if (checked) selectedTypes.add(id) else selectedTypes.remove(id)
                        scheduleFilter()
                    }
                })
            }
            suppressChanges = false
            updateFilterSummary()
            if (initialize) refreshFilters(1)
        }
        searchModel.state.observe(this) { state ->
            if (usingSearch) renderSearch(state)
            else if (state.error != null) { binding.subtitle.text = state.error; retrySearch.visibility = View.VISIBLE }
        }
        searchModel.loadOptions()
        model.address.observe(this) { renderLocation() }

        binding.backButton.setOnClickListener { finish() }
        binding.state.stateRetry.setOnClickListener { model.load() }
        model.recommendations.observe(this, ::render)
        model.savingPlaceId.observe(this) { renderSaving(it) }
        model.outcome.observe(this) { event -> event.take()?.let(::handle) }
        if (model.recommendations.value == null) model.load()
        if (usingSearch && searchModel.lastPath == null) refreshFilters(filterPage)
    }

    private fun buildFilters(saved: Bundle?) {
        usingSearch = saved?.getBoolean("filtered") ?: false
        filterPage = saved?.getInt("filterPage", 1) ?: 1
        selectedTypes.addAll(saved?.getLongArray("selectedTypes")?.toList().orEmpty())
        preferencesInitialized = saved?.getBoolean("preferencesInitialized") ?: false
        val sheet = com.project.bcv.databinding.SheetDestinationFiltersBinding.inflate(layoutInflater)
        filterSheet = sheet
        typeChips = sheet.destinationTypes
        distanceInput = sheet.maxDistance
        daysInput = sheet.maxDays
        distanceInput.setText(saved?.getString("distance").orEmpty())
        daysInput.setText(saved?.getString("days").orEmpty())
        val queryField = com.google.android.material.textfield.TextInputLayout(this).apply {
            hint = "Tìm tên địa danh…"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat())
            boxBackgroundColor = getColor(R.color.field_bg)
            setBoxStrokeColorStateList(getColorStateList(R.color.destination_field_stroke))
        }
        searchInput = com.google.android.material.textfield.TextInputEditText(this).apply {
            id = R.id.destinationQuery; isSingleLine = true
            background = null
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setText(saved?.getString("query").orEmpty()); setTextColor(getColor(R.color.forest))
        }
        queryField.addView(searchInput)
        binding.discoveryFilters.addView(queryField)
        preferInterests = com.google.android.material.checkbox.MaterialCheckBox(this).apply {
            id = R.id.destinationPreference
            text = "Ưu tiên sở thích của tôi"; setTextColor(getColor(R.color.forest))
            buttonTintList = ColorStateList.valueOf(getColor(R.color.teal))
            isChecked = saved?.getBoolean("preferInterests", true) ?: true
            setOnCheckedChangeListener { _, _ -> scheduleFilter() }
        }
        binding.discoveryFilters.addView(preferInterests)
        filterDialog = BottomSheetDialog(this).apply {
            setContentView(sheet.root)
            setOnShowListener {
                val container = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!
                container.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
                container.layoutParams.height = (resources.displayMetrics.heightPixels * 0.9).toInt()
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(container).apply {
                    state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        filterToggle = com.google.android.material.button.MaterialButton(this, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            id = R.id.destinationFilterToggle; isAllCaps = false
            text = "Chọn loại địa danh và bộ lọc"
            setOnClickListener { filterDialog.show() }
        }
        binding.discoveryFilters.addView(filterToggle)
        sheet.closeFilters.setOnClickListener { filterDialog.dismiss() }
        sheet.clearTypes.setOnClickListener {
            suppressChanges = true; selectedTypes.clear(); syncTypeChecks(); suppressChanges = false
            refreshFilters(1)
        }
        sheet.resetFilters.setOnClickListener {
            suppressChanges = true
            searchInput.text.clear(); distanceInput.text.clear(); daysInput.text.clear()
            selectedTypes.clear(); selectedTypes.addAll(profileTypes); syncTypeChecks()
            preferInterests.isChecked = true
            suppressChanges = false; debounce?.cancel(); refreshFilters(1)
        }
        sheet.showFilterResults.setOnClickListener {
            debounce?.cancel(); refreshFilters(1); filterDialog.dismiss()
        }
        listOf(searchInput, distanceInput, daysInput).forEach { input ->
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { scheduleFilter() }
            })
        }
        val paging = LinearLayout(this)
        previousPage = Button(this).apply { text = "Trang trước"; visibility = View.GONE; setOnClickListener { refreshFilters(filterPage - 1) } }
        nextPage = Button(this).apply { text = "Trang sau"; visibility = View.GONE; setOnClickListener { refreshFilters(filterPage + 1) } }
        retrySearch = Button(this).apply { text = "Thử lại"; visibility = View.GONE; setOnClickListener { searchModel.loadOptions(); if (usingSearch) refreshFilters(filterPage) else model.load() } }
        paging.addView(previousPage); paging.addView(nextPage)

        binding.resultsFooter.addView(paging); binding.resultsFooter.addView(retrySearch)
        showMap = Button(this).apply {
            text = "Xem danh sách trên bản đồ"; isEnabled = false
            setOnClickListener { setResult(RESULT_OK, Intent().putExtra("mapPlaceIds", shownPlaceIds)); finish() }
        }
        binding.resultsFooter.addView(showMap)
        if (saved?.getBoolean("expanded") == true) filterToggle.post { filterDialog.show() }
    }

    override fun onDestroy() {
        filterDialog.dismiss()
        super.onDestroy()
    }

    private fun scheduleFilter() {
        if (suppressChanges) return
        debounce?.cancel()
        debounce = lifecycleScope.launch { delay(300); refreshFilters(1) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun syncTypeChecks() {
        for (i in 0 until typeChips.childCount) {
            val chip = typeChips.getChildAt(i) as Chip
            chip.isChecked = chip.tag as Long in selectedTypes
        }
    }

    private fun updateFilterSummary() {
        filterSheet.typeSelectionSummary.text = "Loại địa danh · Đã chọn ${selectedTypes.size}/12"
        val count = selectedTypes.size + (if (distanceInput.text.isNotBlank()) 1 else 0) + (if (daysInput.text.isNotBlank()) 1 else 0)
        filterToggle.text = if (count == 0) "Bộ lọc · Tất cả địa danh" else "Bộ lọc · $count lựa chọn"
    }

    private fun refreshFilters(page: Int) {
        updateFilterSummary()
        filterPage = page.coerceAtLeast(1)
        usingSearch = preferencesInitialized || searchInput.text.isNotBlank() || distanceInput.text.isNotBlank() || daysInput.text.isNotBlank() || selectedTypes.isNotEmpty() || !preferInterests.isChecked || filterPage > 1
        retrySearch.visibility = View.GONE
        if (!usingSearch) {
            previousPage.visibility = View.GONE; nextPage.visibility = View.GONE
            model.recommendations.value?.let(::render) ?: model.load()
            return
        }
        val query = mutableListOf("pageSize=20", "page=$filterPage", "onlyUndiscovered=true", "sort=${if (preferInterests.isChecked) "interests" else "distance"}")
        fun add(key: String, value: String) { if (value.isNotBlank()) query.add("$key=${URLEncoder.encode(value.trim(), "UTF-8")}") }
        add("q", searchInput.text.toString()); add("maxDistanceKm", distanceInput.text.toString()); add("maxEstimatedDays", daysInput.text.toString())
        if (selectedTypes.isNotEmpty()) add("interestGroupIds", selectedTypes.sorted().joinToString(","))
        searchModel.load("api/explore/places?${query.joinToString("&")}")
    }

    private fun renderSearch(state: ExplorerToolsState) {
        Skeleton.hide(binding.skeleton.root)
        binding.state.root.visibility = View.GONE; binding.content.visibility = View.VISIBLE
        previousPage.isEnabled = !state.loading && filterPage > 1; nextPage.isEnabled = false
        retrySearch.visibility = if (state.error != null) View.VISIBLE else View.GONE
        if (state.loading || state.error != null) {
            binding.cards.removeAllViews(); cards.clear()
            showMap.isEnabled = false
            binding.subtitle.text = if (state.loading) "Đang tìm địa danh…" else state.error
            return
        }
        val json = state.result ?: return
        val total = json.getLong("total"); val array = json.getJSONArray("places")
        previousPage.visibility = View.VISIBLE; nextPage.visibility = View.VISIBLE
        nextPage.isEnabled = filterPage * 20 < total
        binding.subtitle.text = if (total == 0L) "Không có địa danh phù hợp. Hãy đổi từ khóa hoặc bộ lọc." else "$total địa danh · Trang $filterPage${if (preferInterests.isChecked) " · Ưu tiên sở thích" else " · Gần nhất"}"
        showCards((0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            RecommendedPlace(index + 1, row.getLong("placeId"), row.getString("name"), row.getString("regionName"),
                row.optString("imageUrl").takeUnless { it.isBlank() || it == "null" }, row.getBoolean("isDiscovered"),
                row.getDouble("distanceKm"), row.getInt("requiredSteps"), emptyList(),
                listOfNotNull(if (row.getBoolean("matchesInterests")) "Phù hợp sở thích của bạn" else null, "Ước tính ${row.getInt("estimatedDays")} ngày"))
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("preferencesInitialized", preferencesInitialized)
        outState.putString("query", searchInput.text.toString()); outState.putString("distance", distanceInput.text.toString()); outState.putString("days", daysInput.text.toString())
        outState.putBoolean("preferInterests", preferInterests.isChecked); outState.putLongArray("selectedTypes", selectedTypes.toLongArray())
        outState.putBoolean("filtered", usingSearch); outState.putInt("filterPage", filterPage)
        outState.putBoolean("expanded", filterDialog.isShowing)
        super.onSaveInstanceState(outState)
    }

    private fun render(state: Remote<Recommendations>) {
        if (usingSearch) { renderLocation(); return }
        if (state != Remote.Loading) Skeleton.hide(binding.skeleton.root)
        when (state) {
            Remote.Loading -> Skeleton.show(binding.skeleton.root, binding.content, binding.state.root)
            is Remote.Failed -> {
                binding.state.root.visibility = View.GONE; binding.content.visibility = View.VISIBLE
                binding.cards.removeAllViews(); cards.clear(); showMap.isEnabled = false
                binding.subtitle.text = state.message; retrySearch.visibility = View.VISIBLE
            }
            Remote.SessionExpired -> {
                startActivity(Intent(this, AccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
            is Remote.Ready -> {
                retrySearch.visibility = View.GONE
                val result = state.value
                if (result.places.isEmpty()) {
                    binding.state.root.visibility = View.GONE; binding.content.visibility = View.VISIBLE
                    binding.cards.removeAllViews(); cards.clear()
                    showMap.isEnabled = false
                    binding.subtitle.text = "Chưa có gợi ý phù hợp. Bạn có thể tìm tên hoặc chọn loại địa danh ở trên."
                    return
                }
                binding.state.showContent(binding.content)
                model.describe(result.location)
                renderLocation()
                binding.subtitle.text = "${result.places.size} gợi ý theo sở thích · Đổi lựa chọn ở trên để khám phá thêm"
                showCards(result.places)
            }
        }
    }

    /** The street address like on Explore; the region until it is known (or when Goong has none). */
    private fun renderLocation() {
        val region = (model.recommendations.value as? Remote.Ready)?.value?.regionName
        binding.regionName.text = model.address.value ?: region.orEmpty()
    }

    private fun showCards(places: List<RecommendedPlace>) {
        shownPlaceIds = places.map { it.id }.toLongArray()
        showMap.isEnabled = places.isNotEmpty()
        binding.cards.removeAllViews()
        cards.clear()
        places.forEachIndexed { index, place ->
            val item = ItemRecommendationBinding.inflate(LayoutInflater.from(this), binding.cards, true)
            cards[place.id] = item
            val best = index == 0
            val accent = STATUS_COLORS[index % STATUS_COLORS.size]
            item.card.setCardBackgroundColor(if (best) 0xFFF0F6EE.toInt() else 0xFFFFFFFF.toInt())
            item.card.strokeColor = if (best) 0xFFA9CBB0.toInt() else 0xFFDCE6DD.toInt()
            item.rank.text = "%02d".format(place.rank)
            item.rank.setTextColor(if (best) 0xFF07836D.toInt() else 0xFF85968F.toInt())
            item.status.text = if (place.isDiscovered) "ĐÃ MỞ KHÓA" else "CHƯA MỞ KHÓA"
            item.status.setTextColor(accent)
            item.name.text = place.name
            item.distance.text = "${distanceText(place.distanceKm)} · ~${stepsText(place.requiredSteps)} bước"
            val reasons = place.reasons.ifEmpty {
                if (place.matchedPreferences.isEmpty()) emptyList() else
                    listOf("Hợp sở thích: ${place.matchedPreferences.joinToString(", ")}")
            }
            item.matched.text = reasons.take(2).joinToString(" · ")
            item.matched.contentDescription = reasons.joinToString(". ")
            // Keeps its line when empty so every card has the skeleton's height.
            item.matched.visibility = if (reasons.isEmpty()) View.INVISIBLE else View.VISIBLE
            item.selectButton.backgroundTintList = ColorStateList.valueOf(
                if (best) 0xFF07836D.toInt() else 0xFFEFF2EA.toInt())
            item.selectButton.setTextColor(if (best) 0xFFFFFFFF.toInt() else 0xFF07836D.toInt())
            item.selectButton.contentDescription = "Chọn ${place.name} làm điểm đến"
            item.selectButton.setOnClickListener {
                if(place.isDiscovered) item.card.performClick()
                else if(place.requiredSteps>0)model.select(place.id,place.name)
            }
            item.rewardLock.visibility=if(place.isDiscovered)View.GONE else View.VISIBLE
            item.image.alpha=if(place.isDiscovered)1f else .65f
            item.image.contentDescription="Ảnh ${place.name}${if(place.isDiscovered) "" else " · Xem trước, chưa mở khóa"}"
            item.selectButton.tag=place
            item.card.isFocusable = true
            item.card.contentDescription = "Xem thông tin ${place.name}"
            item.card.setOnClickListener {
                if (model.savingPlaceId.value == null) {
                    detailLauncher.launch(Intent(this, PlaceDetailActivity::class.java)
                        .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.id)
                        .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, place.name)
                        .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED, place.isDiscovered))
                }
            }
            if (Build.VERSION.SDK_INT >= 31 && !place.isDiscovered) {
                item.image.setRenderEffect(RenderEffect.createBlurEffect(4f, 4f, Shader.TileMode.CLAMP))
            }
            place.imageUrl?.let { url -> loadImage(item, url, place.isDiscovered) }
        }
        renderSaving(model.savingPlaceId.value)
    }

    private fun loadImage(item: ItemRecommendationBinding, url: String, discovered: Boolean) {
        lifecycleScope.launch {
            val bitmap = RemoteImages.load(url, sample = 4) ?: return@launch
            item.image.setPadding(0,0,0,0)
            item.image.setContentPadding(0,0,0,0)
            item.image.imageTintList=null
            item.image.scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
            item.image.setImageBitmap(if (Build.VERSION.SDK_INT < 31 && !discovered) blurFallback(bitmap) else bitmap)
        }
    }

    private fun blurFallback(source: Bitmap): Bitmap {
        val small = Bitmap.createScaledBitmap(source, max(1, source.width / 16), max(1, source.height / 16), true)
        return Bitmap.createScaledBitmap(small, source.width, source.height, true)
    }

    private fun renderSaving(placeId: Long?) {
        cards.forEach { (id, item) ->
            val place=item.selectButton.tag as? RecommendedPlace
            item.selectButton.isEnabled = placeId == null && (place?.isDiscovered==true || (place?.requiredSteps?:0)>0)
            item.selectButton.text = when {
                id==placeId -> "Đang lưu…"
                place?.isDiscovered==true -> "Xem địa danh"
                (place?.requiredSteps?:0)<=0 -> "Vị trí hiện tại"
                else -> "Chọn điểm đến"
            }
            if(place?.isDiscovered==true)item.selectButton.contentDescription="Xem địa danh ${place.name} đã mở khóa"
        }
    }

    private fun handle(outcome: SelectOutcome) {
        when (outcome) {
            SelectOutcome.Saved -> {
                setResult(Activity.RESULT_OK)
                finish()
            }
            is SelectOutcome.ConfirmReplace -> confirmReplaceJourney(onReplace = {
                model.select(outcome.placeId, outcome.placeName, replaceActive = true)
            })
            is SelectOutcome.Failed -> Snackbar.make(binding.root, outcome.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun distanceText(km: Double): String =
        NumberFormat.getNumberInstance(vietnamese).apply { maximumFractionDigits = 1 }.format(km) + " km"

    private fun stepsText(steps: Int): String =
        NumberFormat.getIntegerInstance(vietnamese).format(steps)

    companion object {
        /** The virtual location's address Explore already looked up, shown at once. */
        const val EXTRA_ADDRESS = "address"

        /** Status label colours of the three cards, as in the design. */
        val STATUS_COLORS = intArrayOf(0xFFA0712B.toInt(), 0xFF3C7DB0.toInt(), 0xFF07836D.toInt())
    }
}
