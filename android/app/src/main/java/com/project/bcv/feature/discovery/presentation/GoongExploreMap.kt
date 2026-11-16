package com.project.bcv.feature.discovery.presentation

import android.animation.ValueAnimator
import android.app.Activity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.content.res.ColorStateList
import android.graphics.Bitmap
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.core.network.MediaUrlPolicy
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.core.map.GoongDirectionsClient
import com.project.bcv.core.map.RoadRoute
import com.project.bcv.feature.discovery.domain.ExploreMapPlace
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
import com.project.bcv.feature.discovery.domain.PhotoLoadFailures
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.symbolSortKey
import org.maplibre.android.style.layers.PropertyFactory.iconPadding
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textMaxWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.sources.GeoJsonOptions
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Live Goong tiles. The server owns the virtual location and journey positions. */
class GoongExploreMap(
    activity: Activity,
    savedState: Bundle?,
    private val onLoaded: () -> Unit,
    private val onFailed: () -> Unit,
    private val onMapTap: () -> Unit,
    private val onPlaceTap: (Long) -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private val mapView: MapView
    private val startMarker = pinMarker(activity)
    private val destinationMarker = badgeMarker(activity, R.drawable.ic_flag_outline, "Điểm đến",
        accent = 0xFFC36E59.toInt(), fill = 0xFFFDF4EF.toInt(), ring = 0xFFEACDBF.toInt(), ringWidth = 1.5f)
    private val progressMarker = badgeMarker(activity, R.drawable.ic_directions_walk, "Theo tiến độ",
        accent = 0xFF3C7DB0.toInt(), fill = 0xFFFFFFFF.toInt(), ring = 0xFF3C7DB0.toInt(), ringWidth = 3.5f)
    private var map: MapLibreMap? = null
    private var snapshot: ExploreSnapshot? = null
    private val photoUrls = mutableMapOf<Long, String>()
    private val photoDiscovered = mutableMapOf<Long, Boolean>()
    private val photoLoading = mutableSetOf<Long>()
    private val photoFailed = PhotoLoadFailures()
    private val photoSlots = Semaphore(3)
    private var styleLoaded = false
    private var placeSource: GeoJsonSource? = null
    private var routeSource: GeoJsonSource? = null
    private val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var routeJob: Job? = null
    private var requestedRoute: String? = null
    private var roadRoute: RoadRoute? = null
    private var coveredBottom = 0
    private var framedKey: String? = null
    private var progressAnimator: ValueAnimator? = null
    private var shownJourneyId: Long? = null
    /** Route fraction currently drawn; trails the confirmed one while the sync animation runs. */
    private var shownFraction: Double? = null

    init {
        MapLibre.getInstance(activity)
        mapView = MapView(activity).apply {
            onCreate(savedState)
            addOnDidFailLoadingMapListener { onFailed() }
        }
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.setAllGesturesEnabled(true)
            readyMap.uiSettings.isCompassEnabled = false
            readyMap.uiSettings.isRotateGesturesEnabled = false
            readyMap.addOnMapClickListener { coordinate ->
                val point = readyMap.projection.toScreenLocation(coordinate)
                val radius = 28 * density
                val zoom = readyMap.cameraPosition.zoom
                // Pins stand above their tip, so their tap target is centred higher.
                val lift = if (zoom >= PHOTO_MIN_ZOOM) PlacePhotoMarker.CENTER_LIFT_DP * density * pinScale(zoom) else 0f
                val nearest = snapshot?.places?.map { place ->
                    val markerPoint = readyMap.projection.toScreenLocation(
                        LatLng(place.coordinates.latitude, place.coordinates.longitude))
                    val up = if (photoUrls[place.id] != null) lift else 0f
                    place.id to hypot(markerPoint.x - point.x, markerPoint.y - up - point.y)
                }?.minByOrNull { it.second }
                if (nearest != null && nearest.second <= radius) { onPlaceTap(nearest.first); true }
                else { onMapTap(); false }
            }
            readyMap.addOnCameraMoveListener { positionMarkers() }
            readyMap.addOnCameraIdleListener { positionMarkers(); loadPlacePhotos() }
            loadStyle()
        }
    }

    /** F76 question stop on the route: [fraction] of the way, LOCKED / OPEN / CORRECT / WRONG. */
    data class QuizStop(val number: Int, val fraction: Double, val state: String)

    private var host: FrameLayout? = null
    private var quizStops = emptyList<QuizStop>()
    private val quizMarkers = mutableListOf<TextView>()
    var onQuizStopTap: (Int) -> Unit = {}

    /** Shows one round marker per question stop along the current journey's road route. */
    fun showQuizStops(stops: List<QuizStop>) {
        if (stops == quizStops) return
        quizStops = stops
        val parent = host ?: return
        while (quizMarkers.size > stops.size) parent.removeView(quizMarkers.removeAt(quizMarkers.lastIndex))
        while (quizMarkers.size < stops.size) {
            val marker = TextView(parent.context).apply {
                gravity = Gravity.CENTER
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                elevation = 5 * density
                visibility = View.INVISIBLE
            }
            // Below the walker and destination badges so those stay readable when they overlap.
            parent.addView(marker, parent.indexOfChild(mapView) + 1,
                FrameLayout.LayoutParams((QUIZ_SIZE * density).toInt(), (QUIZ_SIZE * density).toInt()))
            quizMarkers += marker
        }
        stops.forEachIndexed { index, stop ->
            val (text, fill, ring, color) = when (stop.state) {
                "OPEN" -> listOf("?", 0xFFFFC94D.toInt(), 0xFFFFFFFF.toInt(), 0xFF5A3B00.toInt())
                "CORRECT" -> listOf("✓", 0xFF07836D.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
                "WRONG" -> listOf("✕", 0xFFB8C4BD.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
                else -> listOf("${stop.number}", 0xFFFFFFFF.toInt(), 0xFFB8C4BD.toInt(), 0xFF85968F.toInt())
            }
            quizMarkers[index].apply {
                this.text = text as String
                setTextColor(color as Int)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(fill as Int)
                    setStroke((2.5f * density).toInt(), ring as Int)
                }
                contentDescription = "Trạm câu hỏi ${stop.number}: " + when (stop.state) {
                    "OPEN" -> "đã mở, chạm để trả lời"; "CORRECT" -> "đã trả lời đúng"
                    "WRONG" -> "đã trả lời"; else -> "chưa tới"
                }
                setOnClickListener { onQuizStopTap(stop.number) }
            }
        }
        positionMarkers()
    }

    fun attach(host: FrameLayout) {
        this.host = host
        host.addView(mapView, 0, FrameLayout.LayoutParams(-1, -1))
        listOf(destinationMarker, progressMarker, startMarker).forEach { marker ->
            host.addView(marker, FrameLayout.LayoutParams(-2, -2))
            // Markers are anchored by their measured size, which is only known after layout.
            marker.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> positionMarkers() }
        }
        // No +/− buttons: pinch-to-zoom is enough and the buttons crowded the map.
    }

    fun show(value: ExploreSnapshot) {
        snapshot = value
        updateMap()
    }

    /** The recenter button: glides back instead of jumping. */
    fun recenter() = frameCamera(force = true, animate = true, durationMs = 600)

    fun focusSearchResults() {
        val places = snapshot?.places.orEmpty()
        if (places.isEmpty()) return
        if (places.size == 1) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(places[0].coordinates.latitude, places[0].coordinates.longitude), 13.0))
        } else {
            val bounds = LatLngBounds.Builder()
            places.forEach { bounds.include(LatLng(it.coordinates.latitude, it.coordinates.longitude)) }
            // Keep results clear of the location card (top) and the collapsed journey sheet (bottom).
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),
                (40 * density).toInt(), (120 * density).toInt(), (40 * density).toInt(), (190 * density).toInt()))
        }
    }

    fun retry() {
        routeJob?.cancel()
        requestedRoute = null
        roadRoute = null
        loadStyle()
    }

    private fun loadStyle() {
        val key = BuildConfig.GOONG_MAPTILES_KEY
        val active = map ?: return
        if (key.isBlank()) {
            onFailed()
            return
        }
        styleLoaded = false
        placeSource = null
        routeSource = null
        active.setStyle("https://tiles.goong.io/assets/goong_map_web.json?api_key=$key") { style ->
            photoUrls.clear()
            photoDiscovered.clear()
            photoFailed.clear()
            styleLoaded = true
            installRouteLayers(style)
            installPlaceLayers(style)
            updateMap()
            onLoaded()
        }
    }

    private fun updateMap() {
        val value = snapshot ?: return
        if (map == null || !styleLoaded) return
        placeSource?.setGeoJson(placesGeoJson(value.places))
        loadPlacePhotos()
        val location = value.location
        val journey = value.journey?.takeIf { it.status == "IN_PROGRESS" }
        startMarker.visibility = if (location != null) View.VISIBLE else View.GONE
        destinationMarker.visibility = if (location != null && journey != null) View.VISIBLE else View.GONE
        // At 0 steps the walker would sit on top of the virtual-location pin.
        progressMarker.visibility = if (journey != null && journey.recordedSteps > 0 && roadRoute != null)
            destinationMarker.visibility
            else View.GONE
        if (location == null || journey == null) {
            progressAnimator?.cancel()
            shownJourneyId = null
            shownFraction = null
            routeJob?.cancel()
            requestedRoute = null
            roadRoute = null
            routeSource?.setGeoJson(EMPTY_COLLECTION)
            frameCamera()
            mapView.post { positionMarkers() }
            return
        }
        val start = LatLng(location.coordinates.latitude, location.coordinates.longitude)
        val destination = LatLng(journey.destination.latitude, journey.destination.longitude)
        ensureRoute(start, destination)
        showProgress(journey)
        frameCamera()
        mapView.post { positionMarkers() }
    }

    /**
     * Draws the walked part of the route. After a step sync on the same journey the walker and the
     * green line travel from the old fraction to the new one instead of jumping there.
     */
    private fun showProgress(journey: ExploreJourney) {
        val route = roadRoute
        val target = progressFraction(journey)
        val from = shownFraction
        if (route == null) {
            routeSource?.setGeoJson(EMPTY_COLLECTION)
            return
        }
        val sameJourney = shownJourneyId == journey.id && from != null
        shownJourneyId = journey.id
        if (sameJourney && from == target && progressAnimator?.isRunning != true) {
            drawProgress(route, target)
            return
        }
        progressAnimator?.cancel()
        if (!sameJourney || from == null) {
            drawProgress(route, target)
            return
        }
        progressAnimator = ValueAnimator.ofFloat(from.toFloat(), target.toFloat()).apply {
            duration = PROGRESS_ANIMATION_MS
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { drawProgress(route, (it.animatedValue as Float).toDouble()) }
            start()
        }
    }

    private fun drawProgress(route: RoadRoute, fraction: Double) {
        shownFraction = fraction
        routeSource?.setGeoJson(route.progressGeoJson(fraction))
        positionMarkers()
    }

    /**
     * Moves the camera only when what it frames changes (another journey, the road route arriving,
     * a new virtual location) or when [force]d. Step syncs and sheet drags leave the user's view alone.
     */
    private fun frameCamera(force: Boolean = false, animate: Boolean = false, durationMs: Int = 350) {
        val value = snapshot ?: return
        val location = value.location ?: return
        val journey = value.journey?.takeIf { it.status == "IN_PROGRESS" }
        val key = if (journey == null) "here:${location.coordinates}"
            else "trip:${journey.id}:${location.coordinates}:${roadRoute != null}"
        if (!force && key == framedKey) return
        framedKey = key
        if (journey == null) {
            updateCamera(animate, durationMs)
            return
        }
        val start = LatLng(location.coordinates.latitude, location.coordinates.longitude)
        val destination = LatLng(journey.destination.latitude, journey.destination.longitude)
        val bounds = roadRoute?.bounds() ?: LatLngBounds.Builder().include(start).include(destination).build()
        mapView.post {
            // Fit the whole route (start, walker, destination) between the location card and the sheet.
            val side = (36 * density).toInt()
            val update = CameraUpdateFactory.newLatLngBounds(bounds, side, (104 * density).toInt(),
                side, coveredBottom + (28 * density).toInt())
            if (animate) map?.animateCamera(update, durationMs) else map?.moveCamera(update)
            positionMarkers()
        }
    }

    /**
     * Height in pixels of the map hidden by the bottom sheet in its settled state. The caller sends it
     * only when the sheet settles (never mid-drag); the route then glides to fit the visible part.
     */
    fun setCoveredBottom(pixels: Int) {
        if (pixels <= 0 || pixels == coveredBottom) return
        val firstKnown = coveredBottom == 0
        coveredBottom = pixels
        frameCamera(force = true, animate = !firstKnown)
    }

    private fun ensureRoute(start: LatLng, destination: LatLng) {
        val key = "${start.latitude},${start.longitude}|${destination.latitude},${destination.longitude}"
        if (requestedRoute == key) return
        requestedRoute = key
        roadRoute = null
        routeJob?.cancel()
        routeJob = routeScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                GoongDirectionsClient.route(start, destination, BuildConfig.GOONG_API_KEY)
            }
            if (requestedRoute == key) {
                roadRoute = loaded
                updateMap()
            }
        }
    }

    private fun progressFraction(journey: ExploreJourney) = if (journey.requiredSteps <= 0) 0.0
        else (journey.recordedSteps.toDouble() / journey.requiredSteps).coerceIn(0.0, 1.0)

    private fun positionMarkers() {
        val active = map ?: return
        val value = snapshot ?: return
        val location = value.location ?: return
        val start = LatLng(location.coordinates.latitude, location.coordinates.longitude)
        placeMarker(startMarker, active.projection.toScreenLocation(start), anchorY = startMarker.height.toFloat())
        val journey = value.journey?.takeIf { it.status == "IN_PROGRESS" }
        val stopRoute = roadRoute
        quizMarkers.forEachIndexed { index, marker ->
            val stop = quizStops.getOrNull(index)
            if (journey == null || stopRoute == null || stop == null) { marker.visibility = View.INVISIBLE; return@forEachIndexed }
            placeMarker(marker, active.projection.toScreenLocation(stopRoute.pointAt(stop.fraction)), QUIZ_SIZE * density / 2)
        }
        journey ?: return
        val destination = LatLng(journey.destination.latitude, journey.destination.longitude)
        val badgeCenter = BADGE_SIZE * density / 2
        placeMarker(destinationMarker, active.projection.toScreenLocation(destination), badgeCenter)
        val fraction = shownFraction ?: progressFraction(journey)
        val route = roadRoute?.takeIf { fraction > 0.0 }
        val progress = route?.pointAt(fraction)
        if (progress != null) {
            placeMarker(progressMarker, active.projection.toScreenLocation(progress), badgeCenter)
            // The walk icon faces right; mirror only the icon (not its label) when the road heads west.
            route.headsEastAt(fraction)?.let { east -> progressMarker.getChildAt(0).scaleX = if (east) 1f else -1f }        } else progressMarker.visibility = View.GONE
    }

    /** Puts [view] so the point [anchorY] pixels below its top edge sits on [point]. */
    private fun placeMarker(view: View, point: PointF, anchorY: Float) {
        view.x = point.x - view.width / 2f
        view.y = point.y - anchorY
        view.visibility = if (point.x in 0f..mapView.width.toFloat() &&
            point.y in 0f..mapView.height.toFloat()) View.VISIBLE else View.INVISIBLE
    }

    private fun installRouteLayers(style: Style) {
        routeSource = GeoJsonSource(ROUTE_SOURCE, EMPTY_COLLECTION)
        style.addSource(routeSource!!)
        style.addLayer(LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE)
            .withProperties(lineColor(0xFFFFFFFF.toInt()), lineWidth(9f),
                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
        style.addLayer(LineLayer(ROUTE_REMAINING_LAYER, ROUTE_SOURCE)
            .withFilter(Expression.eq(Expression.get("part"), Expression.literal("remaining")))
            .withProperties(lineColor(0xFF86A9A6.toInt()), lineWidth(5f),
                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
        style.addLayer(LineLayer(ROUTE_DONE_LAYER, ROUTE_SOURCE)
            .withFilter(Expression.eq(Expression.get("part"), Expression.literal("done")))
            .withProperties(lineColor(0xFF07836D.toInt()), lineWidth(5.5f),
                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
    }

    private fun loadPlacePhotos() {
        if (!styleLoaded) return
        val active = map ?: return
        if (active.cameraPosition.zoom < PHOTO_MIN_ZOOM) return
        val style = active.style ?: return
        val places = snapshot?.places ?: return
        places.filter { place ->
            val point = active.projection.toScreenLocation(LatLng(place.coordinates.latitude, place.coordinates.longitude))
            place.imageUrl != null && (photoUrls[place.id] != place.imageUrl || photoDiscovered[place.id] != place.isDiscovered) && place.id !in photoLoading &&
                !photoFailed.contains(place.id, place.imageUrl, place.isDiscovered) && point.x in -50f..(mapView.width + 50f) && point.y in -50f..(mapView.height + 50f)
        }.take(30).forEach { place ->
            val url = place.imageUrl ?: return@forEach
            photoLoading.add(place.id)
            routeScope.launch {
                try {
                    val bitmap = photoSlots.withPermit {
                        val privateUrl = MediaUrlPolicy.authenticatedUrl(BuildConfig.API_BASE_URL, url)
                        if (privateUrl != null) SessionPreferences(mapView.context).token?.let {
                            RemoteImages.loadAuthenticated(BuildConfig.API_BASE_URL, privateUrl, it, 4)
                        } else RemoteImages.load(url, 4)
                    }
                    if (bitmap == null) { photoFailed.add(place.id, url, place.isDiscovered); return@launch }
                    if (active.style !== style) return@launch
                    if (snapshot?.places?.firstOrNull { it.id == place.id }?.isDiscovered != place.isDiscovered) return@launch
                    if (photoUrls.size >= 100 && place.id !in photoUrls) {
                        val oldest = photoUrls.keys.first()
                        photoUrls.remove(oldest)
                        photoDiscovered.remove(oldest)
                        style.removeImage("place-photo-$oldest")
                    }
                    val output = PlacePhotoMarker.render(bitmap, place.isDiscovered, density)
                    style.addImage("place-photo-${place.id}", output)
                    photoUrls[place.id] = url
                    photoDiscovered[place.id] = place.isDiscovered
                    placeSource?.setGeoJson(placesGeoJson(snapshot?.places.orEmpty()))
                } finally {
                    photoLoading.remove(place.id)
                    // Keep going: only 30 load per pass, the rest of the visible places follow
                    // without waiting for the next camera move. Loaded and failed ones are skipped.
                    loadPlacePhotos()
                }
            }
        }
    }

    /**
     * Places on the map, by zoom:
     *  - places without a photo are a small dot (teal discovered, grey not yet); from
     *    [PHOTO_MIN_ZOOM] places with a photo are a pin, the name underneath from [LABEL_MIN_ZOOM];
     *  - zoomed out (below [THIN_UNTIL_ZOOM]) overlapping dots and pins thin out, discovered first,
     *    so the map never turns into a carpet; from a city view up every place shows.
     * The base map's own shop / restaurant / station icons are hidden so the places stand out.
     */
    private fun installPlaceLayers(style: Style) {
        style.layers.filter { it.id.startsWith("poi-") && it.id !in KEPT_BASE_POI }
            .forEach { it.setProperties(visibility(Property.NONE)) }
        placeSource = GeoJsonSource(PLACE_SOURCE, EMPTY_COLLECTION)
        style.addSource(placeSource!!)

        val noPin = Expression.eq(Expression.get("hasPhoto"), Expression.literal(false))
        // Dots are symbols, not circles, so zoomed out they thin themselves out instead of carpeting
        // the map; from THIN_UNTIL_ZOOM (a city on screen) every place shows. Discovered ones win.
        style.addImage(DOT_DISCOVERED, dot(0xFF07836D.toInt()))
        style.addImage(DOT_LOCKED, dot(0xFF9AA59F.toInt()))
        fun dots(id: String, filter: Expression) = SymbolLayer(id, PLACE_SOURCE)
            .withFilter(filter)
            .withProperties(
                iconImage(Expression.switchCase(Expression.get("isDiscovered"), Expression.literal(DOT_DISCOVERED), Expression.literal(DOT_LOCKED))),
                iconSize(zoomScale(5f to 0.75f, 12f to 1f)),
                iconAllowOverlap(Expression.step(Expression.zoom(), Expression.literal(false), Expression.stop(THIN_UNTIL_ZOOM, true))),
                iconPadding(6f),
                symbolSortKey(Expression.switchCase(Expression.get("isDiscovered"), Expression.literal(0f), Expression.literal(1f))))
        // Places with a photo are a pin from PHOTO_MIN_ZOOM; further out they are a dot like the rest.
        style.addLayer(dots("place-far-photo-dots", Expression.eq(Expression.get("hasPhoto"), Expression.literal(true)))
            .apply { maxZoom = PHOTO_MIN_ZOOM })
        style.addLayer(dots(EXPLORED_DOT_LAYER, noPin))

        // Only places with a photo get a pin; the rest stay a tidy dot instead of an empty frame.
        style.addLayer(SymbolLayer(PIN_LAYER, PLACE_SOURCE)
            .withFilter(Expression.eq(Expression.get("hasPhoto"), Expression.literal(true)))
            .withProperties(
                iconImage(Expression.get("thumbnail")),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconSize(zoomScale(PHOTO_MIN_ZOOM to PIN_SCALE_MIN, 9f to 0.6f, 11f to 0.72f, 14f to 1f)),
                // Zoomed out, overlapping pins thin out (discovered first); from a city view up all show.
                iconAllowOverlap(Expression.step(Expression.zoom(), Expression.literal(false), Expression.stop(THIN_UNTIL_ZOOM, true))),
                iconPadding(2f),
                textField(Expression.step(Expression.zoom(), Expression.literal(""),
                    Expression.stop(LABEL_MIN_ZOOM, Expression.get("name")))),
                textFont(arrayOf("Roboto Medium")), textSize(12f), textMaxWidth(9f),
                textColor(0xFF19473B.toInt()), textHaloColor(0xFFFFFFFF.toInt()), textHaloWidth(1.6f),
                textAnchor(Property.TEXT_ANCHOR_TOP), textOffset(arrayOf(0f, 0.25f)), textOptional(true),
                // Discovered places win when pins would overlap.
                symbolSortKey(Expression.switchCase(Expression.get("isDiscovered"), Expression.literal(0f), Expression.literal(1f))))
            .apply { minZoom = PHOTO_MIN_ZOOM })
    }

    /** Small round marker: [color] fill with a white ring (5dp + 1.5dp). */
    private fun dot(color: Int): Bitmap {
        val radius = 5 * density; val ring = 1.5f * density
        val size = ((radius + ring) * 2).toInt() + 2
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(size / 2f, size / 2f, radius + ring, paint)
        paint.color = color; canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        return bitmap
    }

    private fun zoomScale(vararg stops: Pair<Float, Float>): Expression = Expression.interpolate(
        Expression.linear(), Expression.zoom(),
        *stops.map { (zoom, scale) -> Expression.stop(zoom, scale) }.toTypedArray())

    /** Pin scale at [zoom], matching the pin layer, for tap targets. */
    private fun pinScale(zoom: Double): Float =
        when {
            zoom >= 14 -> 1f
            zoom >= 11 -> 0.72f + 0.28f * ((zoom - 11) / 3).toFloat()
            zoom >= 9 -> 0.6f + 0.12f * ((zoom - 9) / 2).toFloat()
            else -> PIN_SCALE_MIN + (0.6f - PIN_SCALE_MIN) * ((zoom - PHOTO_MIN_ZOOM) / (9 - PHOTO_MIN_ZOOM)).toFloat().coerceIn(0f, 1f)
        }

    private fun placesGeoJson(places: List<ExploreMapPlace>): String {
        val features = JSONArray()
        places.forEach { place ->
            features.put(JSONObject().put("type", "Feature")
                .put("geometry", JSONObject().put("type", "Point")
                    .put("coordinates", JSONArray().put(place.coordinates.longitude)
                        .put(place.coordinates.latitude)))
                .put("properties", JSONObject().put("placeId", place.id)
                    .put("name", place.name)
                    .put("hasPhoto", photoUrls[place.id] == place.imageUrl && photoDiscovered[place.id] == place.isDiscovered && place.imageUrl != null)
                    .put("thumbnail", "place-photo-${place.id}")
                    .put("isDiscovered", place.isDiscovered)))
        }
        return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
    }

    /** Label above a red pin whose tip sits on the virtual location. */
    private fun pinMarker(activity: Activity): LinearLayout {
        val view = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            elevation = 8 * density
        }
        // Explicit wrap_content: a vertical LinearLayout would otherwise stretch the label to the 40dp pin.
        view.addView(markerLabel(activity, "Vị trí ảo", 0xFFC62828.toInt()), LinearLayout.LayoutParams(-2, -2))
        val icon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_map_marker_red)
            contentDescription = "Vị trí ảo trên bản đồ"
        }
        view.addView(icon, LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt()))
        return view
    }

    /** Round badge centred on its point, with a pill label underneath. */
    private fun badgeMarker(activity: Activity, iconId: Int, label: String, accent: Int, fill: Int,
                            ring: Int, ringWidth: Float): LinearLayout {
        val view = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            elevation = 6 * density
        }
        val icon = ImageView(activity).apply {
            setImageResource(iconId)
            imageTintList = ColorStateList.valueOf(accent)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fill)
                setStroke((ringWidth * density).toInt().coerceAtLeast(1), ring)
            }
            val inset = (10 * density).toInt()
            setPadding(inset, inset, inset, inset)
            contentDescription = label
        }
        val size = (BADGE_SIZE * density).toInt()
        view.addView(icon, LinearLayout.LayoutParams(size, size))
        view.addView(markerLabel(activity, label, accent), LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = (4 * density).toInt()
        })
        return view
    }

    private fun markerLabel(activity: Activity, label: String, color: Int) = TextView(activity).apply {
        text = label
        setTextColor(color)
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        setBackgroundResource(R.drawable.starting_point_card_bg)
        val horizontal = (10 * density).toInt()
        val vertical = (3 * density).toInt()
        setPadding(horizontal, vertical, horizontal, vertical)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Centres the virtual location. The first time it uses a neighbourhood zoom; afterwards it keeps
     * the user's zoom, only pulled into 13–16 so the place stays readable.
     */
    private fun updateCamera(animate: Boolean = false, durationMs: Int = 350) {
        val coordinates = snapshot?.location?.coordinates ?: return
        val active = map ?: return
        val current = active.cameraPosition.zoom
        val zoom = if (current < 3) DEFAULT_ZOOM else current.coerceIn(13.0, 16.0)
        val update = CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .target(LatLng(coordinates.latitude, coordinates.longitude))
            .zoom(zoom)
            .build())
        if (animate) active.animateCamera(update, durationMs) else active.moveCamera(update)
    }

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onLowMemory() = mapView.onLowMemory()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    fun onDestroy() { progressAnimator?.cancel(); routeScope.cancel(); mapView.onDestroy() }

    private companion object {
        const val BADGE_SIZE = 44f
        const val QUIZ_SIZE = 30f
        const val EMPTY_COLLECTION = """{"type":"FeatureCollection","features":[]}"""
        const val PLACE_SOURCE = "bcv-places"
        const val DEFAULT_ZOOM = 13.7
        const val PROGRESS_ANIMATION_MS = 1_000L
        const val EXPLORED_DOT_LAYER = "bcv-explored-dots"
        /** From this zoom (most of the country on screen) places are photo pins; names join from [LABEL_MIN_ZOOM]. */
        const val PHOTO_MIN_ZOOM = 6f
        /** Below this zoom overlapping dots and pins thin out; from here on every place shows. */
        const val THIN_UNTIL_ZOOM = 11
        const val DOT_DISCOVERED = "bcv-dot-discovered"
        const val DOT_LOCKED = "bcv-dot-locked"
        const val LABEL_MIN_ZOOM = 12f
        const val PIN_SCALE_MIN = 0.45f
        const val PIN_LAYER = "bcv-place-pins"
        /** Base-map POIs kept: landmarks that help orientation. Shops, food, stations etc. are hidden. */
        val KEPT_BASE_POI = setOf("poi-mountain", "poi-airport")
        const val ROUTE_SOURCE = "bcv-route"
        const val ROUTE_CASING_LAYER = "bcv-route-casing"
        const val ROUTE_DONE_LAYER = "bcv-route-done"
        const val ROUTE_REMAINING_LAYER = "bcv-route-remaining"
    }
}
