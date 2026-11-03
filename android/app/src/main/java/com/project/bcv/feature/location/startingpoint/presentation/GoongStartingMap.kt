package com.project.bcv.feature.location.startingpoint.presentation

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** A read-only Goong map preview with a red marker. The selected point is owned by the ViewModel. */
class GoongStartingMap(activity: Activity, savedState: Bundle?) {
    private val mapView: MapView
    private val markerSize = (40 * activity.resources.displayMetrics.density).toInt()
    private val marker = ImageView(activity).apply {
        setImageResource(R.drawable.ic_map_marker_red)
        contentDescription = "Điểm xuất phát ảo"
        visibility = View.INVISIBLE
    }
    private var map: MapLibreMap? = null
    private var target: Coordinates? = null
    private var targetZoom = DEFAULT_ZOOM
    private var zoomControls: View? = null

    init {
        MapLibre.getInstance(activity)
        mapView = MapView(activity).apply {
            onCreate(savedState)
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> positionMarker() }
        }
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.setAllGesturesEnabled(true)
            readyMap.uiSettings.isRotateGesturesEnabled = false
            readyMap.uiSettings.isCompassEnabled = false
            readyMap.addOnCameraMoveListener { positionMarker() }
            readyMap.addOnCameraIdleListener { positionMarker() }
            updateCamera(animated = false)
            val key = BuildConfig.GOONG_MAPTILES_KEY
            if (key.isNotBlank()) {
                readyMap.setStyle("https://tiles.goong.io/assets/goong_map_web.json?api_key=$key") {
                    updateCamera(animated = false)
                }
            }
        }
    }

    fun show(host: FrameLayout, coordinates: Coordinates?, zoom: Double = DEFAULT_ZOOM) {
        if (mapView.parent !== host) {
            (zoomControls?.parent as? ViewGroup)?.removeView(zoomControls)
            (mapView.parent as? ViewGroup)?.removeView(mapView)
            (marker.parent as? ViewGroup)?.removeView(marker)
            host.addView(mapView, 0, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            host.addView(marker, 1, FrameLayout.LayoutParams(markerSize, markerSize))
            zoomControls = com.project.bcv.core.map.MapZoomControls.attach(host,
                zoomIn = { map?.animateCamera(CameraUpdateFactory.zoomIn()) },
                zoomOut = { map?.animateCamera(CameraUpdateFactory.zoomOut()) })
        }
        if (coordinates != null && (coordinates != target || zoom != targetZoom)) {
            val animated = target != null
            target = coordinates
            targetZoom = zoom
            mapView.post { updateCamera(animated) }
        }
    }

    private fun updateCamera(animated: Boolean) {
        val coordinates = target ?: return
        val active = map ?: return
        val position = CameraPosition.Builder()
            .target(LatLng(coordinates.latitude, coordinates.longitude))
            .zoom(targetZoom)
            .build()
        if (animated) {
            active.animateCamera(CameraUpdateFactory.newCameraPosition(position), 900)
        } else {
            active.cameraPosition = position
        }
        positionMarker()
    }

    /** Pins the marker's tip to the target coordinate. */
    private fun positionMarker() {
        val active = map
        val coordinates = target
        if (active == null || coordinates == null) {
            marker.visibility = View.INVISIBLE
            return
        }
        val point = active.projection.toScreenLocation(LatLng(coordinates.latitude, coordinates.longitude))
        marker.x = point.x - markerSize / 2f
        marker.y = point.y - markerSize.toFloat()
        marker.visibility = if (point.x in 0f..mapView.width.toFloat() &&
            point.y in 0f..mapView.height.toFloat()) View.VISIBLE else View.INVISIBLE
    }

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onLowMemory() = mapView.onLowMemory()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    fun onDestroy() = mapView.onDestroy()

    companion object {
        /** Quận Thanh Xuân, Hà Nội: shown before the user shares a real location. */
        val DEFAULT_COORDINATES = Coordinates(20.9937, 105.8113)
        const val DEFAULT_ZOOM = 14.0
        const val CURRENT_ZOOM = 16.0
    }
}
