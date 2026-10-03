package com.rnmapbox.rnmbx.modules

import com.facebook.react.bridge.*
import com.rnmapbox.rnmbx.location.LocationManager
import com.rnmapbox.rnmbx.location.HeadingLocationProvider
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.facebook.react.common.LifecycleState
import com.facebook.react.module.annotations.ReactModule
import com.rnmapbox.rnmbx.NativeRNMBXLocationModuleSpec
import com.rnmapbox.rnmbx.location.LocationManager.OnUserLocationChange
import com.rnmapbox.rnmbx.events.LocationEvent
import com.rnmapbox.rnmbx.location.LocationManager.Companion.getInstance
import java.lang.Exception

import com.rnmapbox.rnmbx.v11compat.location.*

data class LocationEventThrottle(var waitBetweenEvents: Double? = null, var lastSentTimestamp: Long? = null) {
    fun shouldSend(nowNanos: Long): Boolean {
        val interval = waitBetweenEvents ?: return true
        val lastSent = lastSentTimestamp ?: return true
        return nowNanos - lastSent > 1_000_000.0 * interval
    }
}

@ReactModule(name = RNMBXLocationModule.REACT_CLASS)
class RNMBXLocationModule(reactContext: ReactApplicationContext) :
    NativeRNMBXLocationModuleSpec(reactContext) {
    private var isEnabled = false
    private var headingUpdatesEnabled = true
    private var hostResumed = reactContext.lifecycleState == LifecycleState.RESUMED
    private val headingProvider by lazy {
        HeadingLocationProvider(DefaultLocationProvider(reactContext.applicationContext)) {
            mLastLocation?.let { location ->
                sendLocationEvent(location, System.currentTimeMillis())
            }
        }
    }
    private var mMinDisplacement = 0f
    private val locationManager: LocationManager? = getInstance(reactContext)
    private var mLastLocation: Location? = null
    private val locationEventThrottle = LocationEventThrottle()

    private val lifecycleEventListener: LifecycleEventListener = object : LifecycleEventListener {
        override fun onHostResume() {
            hostResumed = true
            if (isEnabled) {
                locationManager?.resume()
            }
            updateHeadingSubscription()
        }

        override fun onHostPause() {
            hostResumed = false
            updateHeadingSubscription()
            locationManager?.pause()
        }

        override fun onHostDestroy() {
            hostResumed = false
            updateHeadingSubscription()
            locationManager?.destroy()
        }
    }

    private val onUserLocationChangeCallback: OnUserLocationChange = object : OnUserLocationChange {
        override fun onLocationChange(location: Location?) {
            var changed = (mLastLocation != null) != (location != null)
            val lastLocation = mLastLocation
            if (lastLocation != null && location != null) {
                if (
                    lastLocation.latitude != location.latitude ||
                    lastLocation.longitude != location.longitude ||
                    lastLocation.altitude != location.altitude ||
                    lastLocation.accuracy != location.accuracy ||
                    lastLocation.bearing != location.bearing
                ) {
                    changed = true
                }
            }
            mLastLocation = location
            if (changed && location != null) {
                sendLocationEvent(location)
            }
        }
    }

    init {
        reactContext.addLifecycleEventListener(lifecycleEventListener)
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    @ReactMethod
    override fun start(minDisplacement: Double) {
        UiThreadUtil.runOnUiThread {
            if (isEnabled) {
                setMinDisplacement(minDisplacement)
                return@runOnUiThread
            }
            isEnabled = true
            mMinDisplacement = minDisplacement.toFloat()
            startLocationManager()
            updateHeadingSubscription()
        }
    }

    @ReactMethod
    override fun setMinDisplacement(value: Double) {
        UiThreadUtil.runOnUiThread {
            val minDisplacement = value.toFloat()
            if (mMinDisplacement == minDisplacement) return@runOnUiThread
            mMinDisplacement = minDisplacement
            if (isEnabled) {
                locationManager!!.setMinDisplacement(mMinDisplacement)
            }
        }
    }

    @ReactMethod
    override fun setRequestsAlwaysUse(requestsAlwaysUse: Boolean) {
        // IOS only. Ignored on Android.
    }

    @ReactMethod
    override fun setHeadingUpdatesEnabled(enabled: Boolean) {
        UiThreadUtil.runOnUiThread {
            headingUpdatesEnabled = enabled
            updateHeadingSubscription()
        }
    }

    @ReactMethod
    override fun stop() {
        UiThreadUtil.runOnUiThread {
            stopLocationManager()
            updateHeadingSubscription()
        }
    }

    @ReactMethod
    override fun getLastKnownLocation(promise: Promise) {
        locationManager!!.getLastKnownLocation(
            object : LocationEngineCallback {
                override fun onSuccess(result: LocationEngineResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        val locationEvent = LocationEvent(location, heading = headingProvider.latestHeading)
                        promise.resolve(locationEvent.payload)
                    } else {
                        promise.resolve(null)
                    }
                }

                override fun onFailure(exception: Exception) {
                    promise.reject(exception)
                }
            }
        )
    }

    override fun simulateHeading(changesPerSecond: Double, increment: Double) {
        // ios only
    }

    @ReactMethod
    fun addListener(eventName: String?) {
        // Required for rn built in EventEmitter Calls.
    }

    @ReactMethod
    fun removeListeners(count: Int?) {
        // Required for rn built in EventEmitter Calls.
    }

    private fun startLocationManager() {
        mLastLocation = null;
        locationManager!!.addLocationListener(onUserLocationChangeCallback)
        locationManager.setMinDisplacement(mMinDisplacement)
        locationManager.startCounted()
    }

    private fun updateHeadingSubscription() {
        if (isEnabled && hostResumed && headingUpdatesEnabled) {
            headingProvider.start()
        } else {
            headingProvider.stop()
        }
    }

    private fun sendLocationEvent(location: Location, timestamp: Long = location.timestamp) {
        if (!isEnabled || !shouldSendLocationEvent()) return
        val event = LocationEvent(location, heading = headingProvider.latestHeading, eventTimestamp = timestamp)
        locationEventThrottle.lastSentTimestamp = System.nanoTime()
        emitOnLocationUpdate(event.toJSON())
    }

    private fun stopLocationManager() {
        if (!isEnabled) {
            return
        }
        locationManager!!.removeLocationListener(onUserLocationChangeCallback)
        locationManager.stopCounted()
        isEnabled = false
        mLastLocation = null
    }

    // region Location event throttle
    @ReactMethod
    override fun setLocationEventThrottle(throttleValue: Double) {
        UiThreadUtil.runOnUiThread {
            locationEventThrottle.waitBetweenEvents = throttleValue.takeIf { it > 0 }
        }
    }

    fun shouldSendLocationEvent(): Boolean {
        return locationEventThrottle.shouldSend(System.nanoTime())
    }

    @ReactMethod
    override fun pauseUpdates() {
        locationManager?.pauseUpdates()
    }

    @ReactMethod
    override fun resumeUpdates(clearAll: Boolean) {
        locationManager?.resumeUpdates(clearAll)
    }
    // endregion

    override fun invalidate() {
        UiThreadUtil.runOnUiThread {
            stopLocationManager()
            headingProvider.stop()
            reactApplicationContext.removeLifecycleEventListener(lifecycleEventListener)
        }
        super.invalidate()
    }


    companion object {
        const val REACT_CLASS = "RNMBXLocationModule"
        const val LOCATION_UPDATE = "MapboxUserLocationUpdate"
    }
}
