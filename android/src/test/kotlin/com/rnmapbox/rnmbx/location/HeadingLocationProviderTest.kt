package com.rnmapbox.rnmbx.location

import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.Surface
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class HeadingLocationProviderTest {
    private val sensors = mock<SensorManager>()
    private val headings = mutableListOf<Double>()
    private val provider = HeadingLocationProvider(sensors, { Surface.ROTATION_0 }) { headings.add(it) }
    private val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

    @Test
    fun `start and stop subscribe only to orientation sensors and are idempotent`() {
        val rotation = mock<Sensor>()
        whenever(sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)).thenReturn(rotation)
        whenever(sensors.registerListener(eq(provider), eq(rotation), any<Int>())).thenReturn(true)
        provider.start()
        provider.start()
        verify(sensors).registerListener(provider, rotation, 100_000)
        assertNull(provider.latestHeading)
        assertEquals(emptyList<Double>(), headings)
        provider.stop()
        provider.stop()
        verify(sensors).unregisterListener(provider)
    }

    @Test
    fun `devices without rotation vectors use accelerometer and magnetometer`() {
        val gravity = mock<Sensor>()
        val magnetic = mock<Sensor>()
        whenever(sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(gravity)
        whenever(sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)).thenReturn(magnetic)
        whenever(sensors.registerListener(eq(provider), any<Sensor>(), any<Int>())).thenReturn(true)
        provider.start()
        verify(sensors).registerListener(provider, gravity, 100_000)
        verify(sensors).registerListener(provider, magnetic, 100_000)
        provider.stop()
        verify(sensors).unregisterListener(provider)
    }

    @Test
    fun `failed fallback registration cleans up the first sensor`() {
        val gravity = mock<Sensor>()
        val magnetic = mock<Sensor>()
        whenever(sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(gravity)
        whenever(sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)).thenReturn(magnetic)
        whenever(sensors.registerListener(eq(provider), eq(gravity), any<Int>())).thenReturn(true)
        provider.start()
        verify(sensors).unregisterListener(provider)
        assertNull(provider.latestHeading)
    }

    @Test
    fun `all display rotations are accounted for`() {
        assertEquals(0.0, HeadingLocationProvider.headingFromMatrix(identity, Surface.ROTATION_0))
        assertEquals(270.0, HeadingLocationProvider.headingFromMatrix(identity, Surface.ROTATION_90))
        assertEquals(180.0, HeadingLocationProvider.headingFromMatrix(identity, Surface.ROTATION_180))
        assertEquals(90.0, HeadingLocationProvider.headingFromMatrix(identity, Surface.ROTATION_270))
    }

    @Test
    fun `stationary device rotation changes heading without location data`() {
        val east = floatArrayOf(0f, 1f, 0f, -1f, 0f, 0f, 0f, 0f, 1f)
        assertEquals(90.0, HeadingLocationProvider.headingFromMatrix(east, Surface.ROTATION_0))
    }

    @Test
    fun `upright phone uses the back facing direction`() {
        val upright = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        assertEquals(0.0, HeadingLocationProvider.headingFromMatrix(upright, Surface.ROTATION_0))
    }

    @Test
    fun `invalid orientation does not manufacture a heading`() {
        assertNull(HeadingLocationProvider.headingFromMatrix(FloatArray(9), Surface.ROTATION_0))
        identity[1] = Float.NaN
        assertNull(HeadingLocationProvider.headingFromMatrix(identity, Surface.ROTATION_0))
    }
}
