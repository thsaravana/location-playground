package com.madrapps.location_playground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        val instance = GoogleApiAvailability.getInstance()
        val googlePlayServicesAvailable = instance.isGooglePlayServicesAvailable(this)
        Log.d("GoogleApi", "Available = $googlePlayServicesAvailable")
        val errorDialog = instance.getErrorDialog(this, ConnectionResult.SERVICE_DISABLED, 45)
        errorDialog.show()
    }
}