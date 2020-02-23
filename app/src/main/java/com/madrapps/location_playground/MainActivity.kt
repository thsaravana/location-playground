package com.madrapps.location_playground

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.checkSelfPermission
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_LAST_LOCATION = 33
private const val REQUEST_CHECK_SETTINGS = 44
private const val REQUEST_LOCATION_UPDATE = 55

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.create()?.apply {
        interval = 1000
        fastestInterval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocationAfterPermission()

        // To check location settings, runtime permissions are not required
        locationRequest?.let { checkLocationSettings(it) }
    }

    private fun checkLocationSettings(locationRequest: LocationRequest) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsTask.addOnSuccessListener {
            // Location settings satisfied. Request for location here
            Log.d("LocationRequest", "Success - ${it.locationSettingsStates}")
            startLocationUpdates(fusedLocationClient, locationRequest)
        }
        locationSettingsTask.addOnFailureListener {
            if (it is ResolvableApiException) {
                // Location settings are not satisfied, show dialog to fix this
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                    it.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore error
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            Log.d("LocationRequest", "Result = $resultCode")
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(
                        "LocationRequest",
                        "User enabled settings. Fetch location here"
                    )
                    locationRequest?.let { startLocationUpdates(fusedLocationClient, it) }
                }
                Activity.RESULT_CANCELED -> Log.d(
                    "LocationRequest",
                    "User cancelled. Can't fetch location"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(
        fusedLocationClient: FusedLocationProviderClient,
        locationRequest: LocationRequest
    ) {
        requestLocationPermission(REQUEST_LOCATION_UPDATE) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(p0: LocationResult?) {
                        super.onLocationResult(p0)
                        Log.d("LocationUpdates", "LocationResult = $p0")
                        if (p0 != null) {
                            for (location in p0.locations) {
                                txtLocation.text =
                                    "Lat = ${location.latitude}, Lon = ${location.longitude}"
                            }
                        }
                    }

                    override fun onLocationAvailability(p0: LocationAvailability?) {
                        super.onLocationAvailability(p0)
                        Log.d("LocationUpdates", "LocationAvailability = $p0")
                    }
                },
                Looper.getMainLooper()
            )
        }
    }

    private fun getLastLocationAfterPermission() {
        requestLocationPermission(REQUEST_LAST_LOCATION, this::getLastLocation)
    }

    private fun requestLocationPermission(requestCode: Int, body: () -> Unit) {
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            val permissions = if (SDK_INT >= Q) arrayOf(
                ACCESS_FINE_LOCATION
            ) else arrayOf(ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this, permissions, requestCode)
        } else {
            body()
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                txtLocation.text = "Lat = ${it.latitude}, Lon = ${it.longitude}"
                Log.d("Location", "Lat = ${it.latitude}, Lon = ${it.longitude}")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LAST_LOCATION) {
            if ((grantResults.firstOrNull() == PERMISSION_GRANTED)) {
                getLastLocation()
            }
        } else if (requestCode == REQUEST_LOCATION_UPDATE) {
            if ((grantResults.firstOrNull() == PERMISSION_GRANTED)) {
                locationRequest?.let {
                    startLocationUpdates(fusedLocationClient, it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val instance = GoogleApiAvailability.getInstance()
        val googlePlayServicesAvailable = instance.isGooglePlayServicesAvailable(this)
        Log.d("GoogleApi", "Available = $googlePlayServicesAvailable")
        val errorDialog = instance.getErrorDialog(this, googlePlayServicesAvailable, 45)
        errorDialog?.show()
    }
}