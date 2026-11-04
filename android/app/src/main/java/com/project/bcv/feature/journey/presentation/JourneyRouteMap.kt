package com.project.bcv.feature.journey.presentation

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.core.map.GoongDirectionsClient
import com.project.bcv.core.map.RoadRoute
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Small read-only Goong map: virtual location → walker at the confirmed progress → destination. */
class JourneyRouteMap(activity: Activity, savedState: Bundle?, private val host: FrameLayout) {
    private val density = activity.resources.displayMetrics.density
    private val mapView = MapView(activity)
    private val startPin = ImageView(activity).apply {
        setImageResource(R.drawable.ic_map_marker_red)
        contentDescription = "Vị trí ảo"
    }
    /** Where the journey began; only shown once the virtual location has left it. */
    private val startDot = View(activity).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFFFFFFFF.toInt())
            setStroke((3 * density).toInt(), 0xFF07836D.toInt())
        }
        contentDescription = "Điểm xuất phát"
    }
    private val walker = badge(activity, R.drawable.ic_directions_walk, "Theo tiến độ",
        accent = 0xFF3C7DB0.toInt(), fill = 0xFFFFFFFF.toInt(), ring = 0xFF3C7DB0.toInt(), ringWidth = 3f)
    private val destinationBadge = badge(activity, R.drawable.ic_flag_outline, "Điểm đến",
        accent = 0xFFC36E59.toInt(), fill = 0xFFFDF4EF.toInt(), ring = 0xFFEACDBF.toInt(), ringWidth = 1.5f)
    private var map: MapLibreMap? = null
    private var routeSource: GeoJsonSource? = null
    private var start: Coordinates? = null
    private var virtual: Coordinates? = null
    private var journey: ExploreJourney? = null
    private val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var routeJob: Job? = null
    private var requestedRoute: String? = null
    private var roadRoute: RoadRoute? = null

    init {
        MapLibre.getInstance(activity)
        mapView.onCreate(savedState)
        host.addView(mapView, 0, FrameLayout.LayoutParams(-1, -1))
        val pinSize = (34 * density).toInt()
        val badgeSize = (BADGE_SIZE * density).toInt()
        val dotSize = (14 * density).toInt()
        host.addView(startDot, FrameLayout.LayoutParams(dotSize, dotSize))
        host.addView(destinationBadge, FrameLayout.LayoutParams(badgeSize, badgeSize))
        host.addView(walker, FrameLayout.LayoutParams(badgeSize, badgeSize))
        host.addView(startPin, FrameLayout.LayoutParams(pinSize, pinSize))
        listOf(startDot, startPin, walker, destinationBadge).forEach { it.visibility = View.INVISIBLE }
        mapView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> frame() }
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.setAllGesturesEnabled(false)
            readyMap.uiSettings.isCompassEnabled = false
            readyMap.uiSettings.isLogoEnabled = false
            readyMap.uiSettings.isAttributionEnabled = false
            readyMap.addOnCameraMoveListener { positionMarkers() }
            readyMap.addOnCameraIdleListener { positionMarkers() }
            val key = BuildConfig.GOONG_MAPTILES_KEY
            if (key.isBlank()) return@getMapAsync
            readyMap.setStyle("https://tiles.goong.io/assets/goong_map_web.json?api_key=$key") { style ->
                routeSource = GeoJsonSource(ROUTE_SOURCE, """{"type":"FeatureCollection","features":[]}""")
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
                frame()
            }
        }
    }

    /**
     * [virtualLocation] places the red pin. The route runs from where the journey began, which differs
     * from the virtual location once the journey is completed (it then sits on the destination).
     */
    fun show(virtualLocation: Coordinates, journey: ExploreJourney) {
        this.virtual = virtualLocation
        this.start = journey.start ?: virtualLocation
        this.journey = journey
        frame()
    }

    private fun frame() {
        val active = map ?: return
        val from = start?.let { LatLng(it.latitude, it.longitude) } ?: return
        val trip = journey ?: return
        if (mapView.width == 0) return
        val to = LatLng(trip.destination.latitude, trip.destination.longitude)
        ensureRoute(from, to)
        val fraction = progressFraction(trip)
        routeSource?.setGeoJson(roadRoute?.progressGeoJson(fraction)
            ?: """{"type":"FeatureCollection","features":[]}""")
        val pad = (40 * density).toInt()
        active.moveCamera(CameraUpdateFactory.newLatLngBounds(
            roadRoute?.bounds() ?: LatLngBounds.Builder().include(from).include(to).build(),
            pad, pad, pad, pad))
        positionMarkers()
    }

    private fun ensureRoute(from: LatLng, to: LatLng) {
        val key = "${from.latitude},${from.longitude}|${to.latitude},${to.longitude}"
        if (requestedRoute == key) return
        requestedRoute = key
        roadRoute = null
        routeJob?.cancel()
        routeJob = routeScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                GoongDirectionsClient.route(from, to, BuildConfig.GOONG_API_KEY)
            }
            if (requestedRoute == key) {
                roadRoute = loaded
                frame()
            }
        }
    }

    private fun positionMarkers() {
        val active = map ?: return
        val from = start?.let { LatLng(it.latitude, it.longitude) } ?: return
        val trip = journey ?: return
        val to = LatLng(trip.destination.latitude, trip.destination.longitude)
        val projection = active.projection
        val pin = virtual?.let { LatLng(it.latitude, it.longitude) } ?: from
        // The road route's first point is where the line visibly begins; fall back to the stored start.
        if (pin != from) place(startDot, projection.toScreenLocation(roadRoute?.pointAt(0.0) ?: from), tipAtBottom = false)
        else startDot.visibility = View.INVISIBLE
        place(startPin, projection.toScreenLocation(pin), tipAtBottom = true)
        place(destinationBadge, projection.toScreenLocation(to), tipAtBottom = false)
        val route = roadRoute?.takeIf { trip.recordedSteps > 0 && trip.status == "IN_PROGRESS" }
        val progress = route?.pointAt(progressFraction(trip))
        if (progress != null) {
            place(walker, projection.toScreenLocation(progress), tipAtBottom = false)
            // The walk icon faces right; mirror it when the road heads west. North/south keeps the last facing.
            route.headsEastAt(progressFraction(trip))?.let { east -> walker.scaleX = if (east) 1f else -1f }
        } else walker.visibility = View.INVISIBLE
    }

    private fun place(view: View, point: android.graphics.PointF, tipAtBottom: Boolean) {
        val size = (view.layoutParams as ViewGroup.LayoutParams).width.toFloat()
        view.x = point.x - size / 2
        view.y = point.y - if (tipAtBottom) size else size / 2
        view.visibility = View.VISIBLE
    }

    private fun progressFraction(trip: ExploreJourney) = if (trip.requiredSteps <= 0) 0.0
        else (trip.recordedSteps.toDouble() / trip.requiredSteps).coerceIn(0.0, 1.0)

    private fun badge(activity: Activity, iconId: Int, label: String, accent: Int, fill: Int,
                      ring: Int, ringWidth: Float) = ImageView(activity).apply {
        setImageResource(iconId)
        imageTintList = ColorStateList.valueOf(accent)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((ringWidth * density).toInt().coerceAtLeast(1), ring)
        }
        val inset = (8 * density).toInt()
        setPadding(inset, inset, inset, inset)
        elevation = 4 * density
        contentDescription = label
    }

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onLowMemory() = mapView.onLowMemory()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    fun onDestroy() { routeScope.cancel(); mapView.onDestroy() }

    private companion object {
        const val BADGE_SIZE = 38f
        const val ROUTE_SOURCE = "journey-route"
        const val ROUTE_CASING_LAYER = "journey-route-casing"
        const val ROUTE_DONE_LAYER = "journey-route-done"
        const val ROUTE_REMAINING_LAYER = "journey-route-remaining"
    }
}
