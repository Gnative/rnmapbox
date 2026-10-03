package com.rnmapbox.rnmbx.location

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class PausableLocationProviderTest {
    private val delegate = mock<LocationProvider>()
    private val target = mock<LocationConsumer>()
    private var pauseCount = 0
    private val provider = PausableLocationProvider(delegate, isPaused = { pauseCount > 0 })

    private fun register(): LocationConsumer {
        provider.registerLocationConsumer(target)
        val consumer = argumentCaptor<LocationConsumer>()
        verify(delegate).registerLocationConsumer(consumer.capture())
        return consumer.firstValue
    }

    @Test
    fun `heading updates are forwarded without a location update`() {
        val consumer = register()
        consumer.onBearingUpdated(90.0)
        verify(target).onBearingUpdated(90.0)
    }

    @Test
    fun `nested pauses hold both streams and resume their latest values once`() {
        val consumer = register()
        val first = Point.fromLngLat(16.0, 48.0)
        val latest = Point.fromLngLat(16.1, 48.1)
        pauseCount = 2
        consumer.onLocationUpdated(first)
        consumer.onBearingUpdated(45.0)
        consumer.onLocationUpdated(latest)
        consumer.onBearingUpdated(90.0)
        consumer.onHorizontalAccuracyRadiusUpdated(5.0)
        verifyNoInteractions(target)

        pauseCount -= 1
        provider.resumeUpdates()
        verifyNoInteractions(target)
        pauseCount -= 1
        provider.resumeUpdates()
        verify(target).onLocationUpdated(latest)
        verify(target).onBearingUpdated(90.0)
        verify(target).onHorizontalAccuracyRadiusUpdated(5.0)

        clearInvocations(target)
        provider.resumeUpdates()
        verifyNoInteractions(target)
    }

    @Test
    fun `removing a consumer unregisters the SDK stream and discards pending updates`() {
        val consumer = register()
        pauseCount = 1
        consumer.onBearingUpdated(90.0)
        provider.unRegisterLocationConsumer(target)
        verify(delegate).unRegisterLocationConsumer(consumer)
        pauseCount = 0
        provider.resumeUpdates()
        verifyNoInteractions(target)
    }

    @Test
    fun `each map has its own bearing selection`() {
        val firstDelegate = mock<DefaultLocationProvider>()
        val secondDelegate = mock<DefaultLocationProvider>()
        val first = PausableLocationProvider(firstDelegate, isPaused = { false })
        val second = PausableLocationProvider(secondDelegate, isPaused = { false })
        first.updatePuckBearing(PuckBearing.HEADING)
        second.updatePuckBearing(PuckBearing.COURSE)
        verify(firstDelegate).updatePuckBearing(PuckBearing.HEADING)
        verify(secondDelegate).updatePuckBearing(PuckBearing.COURSE)
        first.updatePuckBearing(null)
        verify(firstDelegate).updatePuckBearing(null)
    }

    @Test
    fun `displacement filters coordinates without suppressing stationary compass updates`() {
        var displacement = 100.0
        val provider = PausableLocationProvider(
            delegate,
            isPaused = { false },
            minDisplacement = { displacement },
        )
        provider.registerLocationConsumer(target)
        val consumers = argumentCaptor<LocationConsumer>()
        verify(delegate).registerLocationConsumer(consumers.capture())
        val consumer = consumers.firstValue
        val first = Point.fromLngLat(16.0, 48.0)
        val nearby = Point.fromLngLat(16.00001, 48.0)
        consumer.onLocationUpdated(first)
        verify(target).onLocationUpdated(first)
        clearInvocations(target)
        consumer.onLocationUpdated(nearby)
        verifyNoInteractions(target)
        consumer.onBearingUpdated(90.0)
        verify(target).onBearingUpdated(90.0)
        displacement = 0.0
        consumer.onLocationUpdated(nearby)
        verify(target).onLocationUpdated(nearby)
    }

    @Test
    fun `registering the same consumer twice does not start another SDK subscription`() {
        register()
        provider.registerLocationConsumer(target)
        val consumers = argumentCaptor<LocationConsumer>()
        verify(delegate).registerLocationConsumer(consumers.capture())
        assertEquals(1, consumers.allValues.size)
    }
}
