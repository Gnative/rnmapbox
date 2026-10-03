package com.rnmapbox.rnmbx.location

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class HeadingLocationProviderTest {
    private val delegate = mock<DefaultLocationProvider>()
    private val headings = mutableListOf<Double>()
    private val provider = HeadingLocationProvider(delegate) { headings.add(it) }

    @Test
    fun `stationary rotation emits compass readings without requiring a coordinate update`() {
        provider.start()
        provider.onBearingUpdated(15.0)
        provider.onBearingUpdated(90.0)
        assertEquals(listOf(15.0, 90.0), headings)
        assertEquals(90.0, provider.latestHeading)
        verify(delegate).updatePuckBearing(PuckBearing.HEADING)
    }

    @Test
    fun `position changes do not manufacture a compass heading`() {
        provider.start()
        provider.onLocationUpdated(Point.fromLngLat(16.0, 48.0))
        assertEquals(emptyList<Double>(), headings)
        assertNull(provider.latestHeading)
    }

    @Test
    fun `invalid and duplicate readings are ignored`() {
        provider.start()
        provider.onBearingUpdated(Double.NaN)
        provider.onBearingUpdated(-1.0)
        provider.onBearingUpdated(360.0)
        provider.onBearingUpdated(Double.POSITIVE_INFINITY)
        provider.onBearingUpdated(45.0)
        provider.onBearingUpdated(45.0)
        assertEquals(listOf(45.0), headings)
    }

    @Test
    fun `stop unregisters sensors and rejects late callbacks then restart works`() {
        provider.start()
        provider.start()
        verify(delegate).registerLocationConsumer(provider)
        provider.onBearingUpdated(45.0)
        provider.stop()
        provider.stop()
        verify(delegate).unRegisterLocationConsumer(provider)
        verify(delegate).updatePuckBearing(null)
        provider.onBearingUpdated(90.0)
        assertEquals(listOf(45.0), headings)
        provider.start()
        provider.onBearingUpdated(180.0)
        assertEquals(listOf(45.0, 180.0), headings)
    }
}
