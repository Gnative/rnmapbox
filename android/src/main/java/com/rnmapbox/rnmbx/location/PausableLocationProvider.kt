package com.rnmapbox.rnmbx.location

import android.animation.ValueAnimator
import com.mapbox.common.location.LocationError
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfMeasurement

/** Keeps the SDK's location and compass streams while allowing map updates to be paused. */
internal class PausableLocationProvider(
    private val delegate: LocationProvider,
    private val isPaused: () -> Boolean,
    private val minDisplacement: () -> Double = { 0.0 },
) : LocationProvider {
    private val consumers = mutableMapOf<LocationConsumer, Consumer>()

    fun updatePuckBearing(bearing: PuckBearing?) {
        // The SDK only updates bearing settings automatically for an unwrapped DefaultLocationProvider.
        (delegate as? DefaultLocationProvider)?.updatePuckBearing(bearing)
    }

    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        if (consumers.containsKey(locationConsumer)) return
        val consumer = Consumer(locationConsumer)
        consumers[locationConsumer] = consumer
        delegate.registerLocationConsumer(consumer)
    }

    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        consumers.remove(locationConsumer)?.let(delegate::unRegisterLocationConsumer)
    }

    fun resumeUpdates() {
        if (isPaused()) return
        consumers.values.toList().forEach { it.resumeUpdates() }
    }

    private inner class Consumer(private val target: LocationConsumer) : LocationConsumer {
        private var pendingLocation: Point? = null
        private var pendingBearing: Double? = null
        private var pendingAccuracy: Double? = null
        private var lastLocation: Point? = null

        override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?) {
            val latest = location.lastOrNull() ?: return
            if (isPaused()) {
                pendingLocation = latest
                return
            }
            pendingLocation = null
            val previous = lastLocation
            val displacement = minDisplacement()
            if (previous != null && displacement > 0 &&
                TurfMeasurement.distance(previous, latest, UNIT_METERS) < displacement) return
            lastLocation = latest
            target.onLocationUpdated(*location, options = options)
        }

        override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) {
            if (isPaused()) {
                pendingBearing = bearing.lastOrNull()
            } else {
                pendingBearing = null
                target.onBearingUpdated(*bearing, options = options)
            }
        }

        override fun onHorizontalAccuracyRadiusUpdated(vararg radius: Double, options: (ValueAnimator.() -> Unit)?) {
            if (isPaused()) {
                pendingAccuracy = radius.lastOrNull()
            } else {
                pendingAccuracy = null
                target.onHorizontalAccuracyRadiusUpdated(*radius, options = options)
            }
        }

        fun resumeUpdates() {
            val location = pendingLocation
            val bearing = pendingBearing
            val accuracy = pendingAccuracy
            pendingLocation = null
            pendingBearing = null
            pendingAccuracy = null
            location?.let {
                onLocationUpdated(it)
            }
            bearing?.let { target.onBearingUpdated(it) }
            accuracy?.let { target.onHorizontalAccuracyRadiusUpdated(it) }
        }

        override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) =
            target.onPuckLocationAnimatorDefaultOptionsUpdated(options)

        override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) =
            target.onPuckBearingAnimatorDefaultOptionsUpdated(options)

        override fun onPuckAccuracyRadiusAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) =
            target.onPuckAccuracyRadiusAnimatorDefaultOptionsUpdated(options)

        override fun onError(error: LocationError) = target.onError(error)
    }
}
