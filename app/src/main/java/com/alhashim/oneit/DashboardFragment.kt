package com.alhashim.oneit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import android.location.Location
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient

class DashboardFragment : Fragment() {
    private lateinit var cookieJar: MyCookieJar
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cookieJar = (requireActivity().application as OneITApplication).cookieJar
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        val textViewWelcome: TextView = view.findViewById(R.id.textViewWelcome)
        val textViewBadgeNumber: TextView = view.findViewById(R.id.textViewBadgeNumber)
        val buttonCheckIn: Button = view.findViewById(R.id.buttonCheckIn)
        val buttonCheckOut: Button = view.findViewById(R.id.buttonCheckOut)
        val buttonTimesheet: Button = view.findViewById(R.id.buttonTimesheet)
        val buttonLogout: Button = view.findViewById(R.id.buttonLogout)

        val sharedPreferences = requireActivity().getSharedPreferences("oneIT", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "")
        val badgeNumber = sharedPreferences.getString("badgeNumber", "")
        val serverUrl = sharedPreferences.getString("serverUrl", "")

        textViewWelcome.text = userName
        textViewBadgeNumber.text = badgeNumber

        buttonCheckIn.setOnClickListener {
            checkLocationPermissionAndProceed {
                performCheckIn(serverUrl, badgeNumber)
            }
        }


        buttonCheckOut.setOnClickListener {
            checkLocationPermissionAndProceed {
                performCheckOut(serverUrl, badgeNumber)
            }
        }

        buttonTimesheet.setOnClickListener{
            findNavController().navigate(R.id.action_dashboardFragment_to_timesheetFragment)
        }
    }

    private fun checkLocationPermissionAndProceed(onPermissionGranted: () -> Unit) {
        when {
            hasLocationPermission() -> {
                onPermissionGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale {
                    requestLocationPermission()
                }
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun showLocationPermissionRationale(onProceed: () -> Unit) {
        Toast.makeText(
            requireContext(),
            "Location permission is required for check-in",
            Toast.LENGTH_LONG
        ).show()
        onProceed()
    }

    private fun performCheckIn(serverUrl: String?, badgeNumber: String?) {
        println("Check-In Request.....")

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val mobileModel = Build.MODEL
        val mobileOS = "Android ${Build.VERSION.RELEASE}"

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    sendCheckInRequest(location, serverUrl, badgeNumber, mobileModel, mobileOS)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Unable to get location. Please check your GPS settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Location error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendCheckInRequest(
        location: Location,
        serverUrl: String?,
        badgeNumber: String?,
        mobileModel: String,
        mobileOS: String
    ) {
        println("latitude: ${location.latitude} & longitude: ${location.longitude} , mobileModel: $mobileModel , mobileOS: $mobileOS")

        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("mobileModel", mobileModel)
            put("mobileOS", mobileOS)
            put("badgeNumber", badgeNumber)
        }

        println("Http post request to ${serverUrl}/api/checkIn")

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${serverUrl}/api/checkIn")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Check-in failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                requireActivity().runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val message = jsonResponse.optString("message", "")
                            Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: JSONException) {
                            Toast.makeText(
                                requireContext(),
                                "Error parsing response",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Check-in failed: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with check-in
                    val sharedPreferences = requireActivity().getSharedPreferences("oneIT", Context.MODE_PRIVATE)
                    val serverUrl = sharedPreferences.getString("serverUrl", "")
                    val badgeNumber = sharedPreferences.getString("badgeNumber", "")
                    performCheckIn(serverUrl, badgeNumber)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Location permission is required for check-in",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    ////----checkout
    private fun performCheckOut(serverUrl: String?, badgeNumber: String?) {
        println("Check-Out Request.....")

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val mobileModel = Build.MODEL
        val mobileOS = "Android ${Build.VERSION.RELEASE}"

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    sendCheckOutRequest(location, serverUrl, badgeNumber, mobileModel, mobileOS)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Unable to get location. Please check your GPS settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Location error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendCheckOutRequest(
        location: Location,
        serverUrl: String?,
        badgeNumber: String?,
        mobileModel: String,
        mobileOS: String
    ) {
        println("latitude: ${location.latitude} & longitude: ${location.longitude} , mobileModel: $mobileModel , mobileOS: $mobileOS")

        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("mobileModel", mobileModel)
            put("mobileOS", mobileOS)
            put("badgeNumber", badgeNumber)
        }

        println("Http post request to ${serverUrl}/api/checkOut")

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${serverUrl}/api/checkOut")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Check-out failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                requireActivity().runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val message = jsonResponse.optString("message", "")
                            Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: JSONException) {
                            Toast.makeText(
                                requireContext(),
                                "Error parsing response",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Check-out failed: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}