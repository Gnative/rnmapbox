package com.rnmapbox.rnmbx.location

import android.animation.ValueAnimator
import com.mapbox.common.location.LocationError
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer

/** A separate SDK subscription so JS heading never depends on a map's course setting. */
internal class HeadingLocationProvider(
    private val provider: DefaultLocationProvider,
    private val onHeadingChanged: (Double) -> Unit,
) : LocationConsumer {
    private var active = false
    @Volatile var latestHeading: Double? = null
        private set

    // Called on the UI thread, as required by the Maps SDK location provider.
    fun start() {
        if (active) return
        active = true
        provider.updatePuckBearing(PuckBearing.HEADING)
        provider.registerLocationConsumer(this)
    }

    fun stop() {
        if (!active) return
        active = false
        provider.unRegisterLocationConsumer(this)
        provider.updatePuckBearing(null)
    }

    override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?) {}

    override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) {
        if (!active) return
        val heading = bearing.lastOrNull() ?: return
        if (!heading.isFinite() || heading < 0.0 || heading >= 360.0 || heading == latestHeading) return
        latestHeading = heading
        onHeadingChanged(heading)
    }

    override fun onHorizontalAccuracyRadiusUpdated(vararg radius: Double, options: (ValueAnimator.() -> Unit)?) {}

    override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {}
    override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {}
    override fun onPuckAccuracyRadiusAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) {}
    override fun onError(error: LocationError) {}
}
