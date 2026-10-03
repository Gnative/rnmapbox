package com.rnmapbox.rnmbx.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.atan2

/** A sensor-only compass: subscribing to JS heading must not start another GPS request. */
internal class HeadingLocationProvider(
    private val sensors: SensorManager,
    private val displayRotation: () -> Int,
    private val onHeadingChanged: (Double) -> Unit,
) : SensorEventListener {
    constructor(context: Context, onHeadingChanged: (Double) -> Unit) : this(
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
        { (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation },
        onHeadingChanged,
    )

    private var active = false
    private var useRotationVector = false
    private var hasGravity = false
    private var hasMagnetic = false
    private var lastTimestamp: Long? = null
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private val matrix = FloatArray(9)
    @Volatile var latestHeading: Double? = null
        private set

    // Start/stop on the UI thread; SensorManager dispatches these listeners on the main looper.
    fun start() {
        if (active) return
        latestHeading = null
        lastTimestamp = null
        hasGravity = false
        hasMagnetic = false
        val rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        useRotationVector = rotation != null && sensors.registerListener(this, rotation, SENSOR_PERIOD_US)
        if (useRotationVector) {
            active = true
            return
        }
        val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val magnetometer = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: return
        active = sensors.registerListener(this, accelerometer, SENSOR_PERIOD_US) &&
            sensors.registerListener(this, magnetometer, SENSOR_PERIOD_US)
        if (!active) sensors.unregisterListener(this)
    }

    fun stop() {
        if (!active) return
        active = false
        sensors.unregisterListener(this)
        latestHeading = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!active) return
        if (useRotationVector) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
                event.sensor.type != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) return
        } else {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    filter(event.values, gravity, hasGravity)
                    hasGravity = true
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    filter(event.values, magnetic, hasMagnetic)
                    hasMagnetic = true
                }
                else -> return
            }
            if (!hasGravity || !hasMagnetic) return
        }
        // Match the SDK compass's maximum broadcast rate without allocating per sensor event.
        lastTimestamp?.let { if (event.timestamp - it < 500_000_000L) return }
        if (useRotationVector) {
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
        } else if (!SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) {
            return
        }
        val heading = headingFromMatrix(matrix, displayRotation()) ?: return
        lastTimestamp = event.timestamp
        if (heading == latestHeading) return
        latestHeading = heading
        onHeadingChanged(heading)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun filter(input: FloatArray, output: FloatArray, initialized: Boolean) {
        for (i in 0..2) output[i] = if (initialized) output[i] + 0.45f * (input[i] - output[i]) else input[i]
    }

    companion object {
        private const val SENSOR_PERIOD_US = 100_000

        /** Project the screen's top onto magnetic north/east, using the back when held upright. */
        internal fun headingFromMatrix(matrix: FloatArray, rotation: Int): Double? {
            var axis = if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) 0 else 1
            var sign = if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180) -1.0 else 1.0
            val up = matrix[6 + axis] * sign
            if (abs(up) > 0.7071067811865476) {
                axis = 2
                sign = if (up > 0) -1.0 else 1.0
            }
            val east = matrix[axis] * sign
            val north = matrix[3 + axis] * sign
            if (!east.isFinite() || !north.isFinite() || east * east + north * north < 1e-12) return null
            return (Math.toDegrees(atan2(east, north)) + 360.0) % 360.0
        }
    }
}
