package com.madrapps.location_playground

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.checkSelfPermission
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_LAST_LOCATION = 33
private const val REQUEST_CHECK_SETTINGS = 44

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getLastLocationAfterPermission()

        // To check location settings, runtime permissions are not required
        checkLocationSettings()
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (locationRequest != null) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

            val settingsClient = LocationServices.getSettingsClient(this)
            val locationSettingsTask = settingsClient.checkLocationSettings(builder.build())
            locationSettingsTask.addOnSuccessListener {
                // Location settings satisfied. Request for location here
                Log.d("LocationRequest", "Success - ${it.locationSettingsStates}")
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            Log.d("LocationRequest", "Result = $resultCode")
            when (resultCode) {
                Activity.RESULT_OK -> Log.d(
                    "LocationRequest",
                    "User enabled settings. Fetch location here"
                )
                Activity.RESULT_CANCELED -> Log.d(
                    "LocationRequest",
                    "User cancelled. Can't fetch location"
                )
            }
        }
    }

    private fun getLastLocationAfterPermission() {
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_COARSE_LOCATION),
                REQUEST_LAST_LOCATION
            )
        } else {
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLastLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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