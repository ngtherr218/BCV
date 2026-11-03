package com.project.bcv.feature.discovery.presentation

import android.animation.ValueAnimator
import android.app.Activity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.content.res.ColorStateList
import android.graphics.Bitmap
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
import com.project.bcv.core.map.GoongDirectionsClient
import com.project.bcv.core.map.RoadRoute
import com.project.bcv.core.map.MapZoomControls
import com.project.bcv.feature.discovery.domain.ExploreMapPlace
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.ExploreSnapshot
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
import org.maplibre.android.style.layers.PropertyFactory.iconPadding
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val radius = 24 * density
                val nearest = snapshot?.places?.map { place ->
                    val markerPoint = readyMap.projection.toScreenLocation(
                        LatLng(place.coordinates.latitude, place.coordinates.longitude))
                    // Explored badges stand above their dot, so their tap target is centred higher.
                    val zoom = readyMap.cameraPosition.zoom
                    val lift = if (place.isDiscovered && zoom >= EXPLORED_BADGE_MIN_ZOOM)
                        26 * density * badgeScale(zoom) else 0f
                    place.id to hypot(markerPoint.x - point.x, markerPoint.y - lift - point.y)
                }?.minByOrNull { it.second }
                if (nearest != null && nearest.second <= radius) { onPlaceTap(nearest.first); true }
                else { onMapTap(); false }
            }
            readyMap.addOnCameraMoveListener { positionMarkers() }
            readyMap.addOnCameraIdleListener { positionMarkers() }
            loadStyle()
        }
    }

    fun attach(host: FrameLayout) {
        host.addView(mapView, 0, FrameLayout.LayoutParams(-1, -1))
        listOf(destinationMarker, progressMarker, startMarker).forEach { marker ->
            host.addView(marker, FrameLayout.LayoutParams(-2, -2))
            // Markers are anchored by their measured size, which is only known after layout.
            marker.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> positionMarkers() }
        }
        MapZoomControls.attach(host, topDp = 74,
            zoomIn = { map?.animateCamera(CameraUpdateFactory.zoomIn()) },
            zoomOut = { map?.animateCamera(CameraUpdateFactory.zoomOut()) })
    }

    fun show(value: ExploreSnapshot) {
        snapshot = value
        updateMap()
    }

    /** The recenter button: glides back instead of jumping. */
    fun recenter() = frameCamera(force = true, animate = true, durationMs = 600)

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
        val journey = value.journey?.takeIf { it.status == "IN_PROGRESS" } ?: return
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

    private fun installPlaceLayers(style: Style) {
        placeSource = GeoJsonSource(PLACE_SOURCE, EMPTY_COLLECTION)
        style.addSource(placeSource!!)
        // Marker sizes follow the zoom: full size at street level, small dots when zoomed out,
        // so places stop swelling over the map and each other.
        style.addImage(LOCKED_IMAGE, lockedDot())
        style.addLayer(SymbolLayer(LOCKED_LAYER, PLACE_SOURCE)
            .withFilter(Expression.eq(Expression.get("isDiscovered"), Expression.literal(false)))
            .withProperties(iconImage(LOCKED_IMAGE),
                iconSize(zoomScale(9f to 0.3f, 12f to 0.6f, 15f to 1f)),
                // Zoomed out, overlapping locked dots thin themselves out; up close every one shows.
                iconAllowOverlap(Expression.step(Expression.zoom(), Expression.literal(false),
                    Expression.stop(13, true))),
                iconPadding(1f)))
        style.addLayer(CircleLayer(EXPLORED_DOT_LAYER, PLACE_SOURCE)
            .withFilter(Expression.eq(Expression.get("isDiscovered"), Expression.literal(true)))
            .withProperties(circleColor(0xFFEE8F3A.toInt()), circleRadius(4f),
                circleStrokeColor(0xFFFFFFFF.toInt()), circleStrokeWidth(1.5f))
            .apply { maxZoom = EXPLORED_BADGE_MIN_ZOOM })
        style.addImage(EXPLORED_IMAGE, exploredBadge())
        // Explored badges draw above locked dots and never hide; the badge's bottom is the orange
        // dot, which sits exactly on the place at every scale.
        style.addLayer(SymbolLayer(EXPLORED_ICON_LAYER, PLACE_SOURCE)
            .withFilter(Expression.eq(Expression.get("isDiscovered"), Expression.literal(true)))
            .withProperties(iconImage(EXPLORED_IMAGE), iconAllowOverlap(true), iconIgnorePlacement(true),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconSize(zoomScale(11f to BADGE_SCALE_MIN, 13f to 0.75f, 15f to 1f)))
            .apply { minZoom = EXPLORED_BADGE_MIN_ZOOM })
    }

    private fun zoomScale(vararg stops: Pair<Float, Float>): Expression = Expression.interpolate(
        Expression.linear(), Expression.zoom(),
        *stops.map { (zoom, scale) -> Expression.stop(zoom, scale) }.toTypedArray())

    /** Badge scale at [zoom], matching the explored icon layer, for tap targets. */
    private fun badgeScale(zoom: Double): Float = when {
        zoom <= 11 -> BADGE_SCALE_MIN
        zoom <= 13 -> BADGE_SCALE_MIN + (0.75f - BADGE_SCALE_MIN) * ((zoom - 11) / 2).toFloat()
        zoom <= 15 -> 0.75f + 0.25f * ((zoom - 13) / 2).toFloat()
        else -> 1f
    }

    /** Red dot with a white ring (10dp + 3dp at full size) for a place not discovered yet. */
    private fun lockedDot(): Bitmap {
        val radius = 10 * density
        val ring = 3 * density
        val size = ((radius + ring) * 2).toInt() + 1
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(size / 2f, size / 2f, radius + ring, paint)
        paint.color = 0xFFDB594C.toInt()
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        return bitmap
    }

    /** Cream circle with the house icon, and an orange dot below it marking the place. */
    private fun exploredBadge(): Bitmap {
        fun px(dp: Float) = dp * density
        val badgeRadius = px(20f)
        val badgeStroke = px(1.5f)
        val dotRadius = px(5f)
        val dotStroke = px(2f)
        val gap = px(3f)
        val width = (badgeRadius + badgeStroke) * 2
        val height = width + gap + (dotRadius + dotStroke) * 2
        val bitmap = Bitmap.createBitmap(width.toInt() + 1, height.toInt() + 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = width / 2
        val cy = badgeRadius + badgeStroke
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFBF6EE.toInt()
        canvas.drawCircle(cx, cy, badgeRadius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = badgeStroke
        paint.color = 0xFFE6D6BE.toInt()
        canvas.drawCircle(cx, cy, badgeRadius, paint)
        ContextCompat.getDrawable(mapView.context, R.drawable.ic_place_explored_house)?.let { icon ->
            val half = px(11f).toInt()
            icon.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
            icon.draw(canvas)
        }
        val dotY = height - dotRadius - dotStroke
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(cx, dotY, dotRadius + dotStroke, paint)
        paint.color = 0xFFEE8F3A.toInt()
        canvas.drawCircle(cx, dotY, dotRadius, paint)
        return bitmap
    }

    private fun placesGeoJson(places: List<ExploreMapPlace>): String {
        val features = JSONArray()
        places.forEach { place ->
            features.put(JSONObject().put("type", "Feature")
                .put("geometry", JSONObject().put("type", "Point")
                    .put("coordinates", JSONArray().put(place.coordinates.longitude)
                        .put(place.coordinates.latitude)))
                .put("properties", JSONObject().put("placeId", place.id)
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
        const val EMPTY_COLLECTION = """{"type":"FeatureCollection","features":[]}"""
        const val PLACE_SOURCE = "bcv-places"
        const val LOCKED_LAYER = "bcv-locked-places"
        const val DEFAULT_ZOOM = 13.7
        const val PROGRESS_ANIMATION_MS = 1_000L
        const val LOCKED_IMAGE = "bcv-locked-dot"
        const val EXPLORED_DOT_LAYER = "bcv-explored-dots"
        /** Below this zoom an explored place is a small orange dot instead of the house badge. */
        const val EXPLORED_BADGE_MIN_ZOOM = 11f
        const val BADGE_SCALE_MIN = 0.55f
        const val EXPLORED_ICON_LAYER = "bcv-explored-icons"
        const val EXPLORED_IMAGE = "bcv-explored-badge"
        const val ROUTE_SOURCE = "bcv-route"
        const val ROUTE_CASING_LAYER = "bcv-route-casing"
        const val ROUTE_DONE_LAYER = "bcv-route-done"
        const val ROUTE_REMAINING_LAYER = "bcv-route-remaining"
    }
}
