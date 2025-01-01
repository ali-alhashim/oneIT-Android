package com.alhashim.oneit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.location.Location
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException

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
                showBiometricPrompt {
                    performCheckIn(serverUrl, badgeNumber)
                }
            }
        }

        buttonCheckOut.setOnClickListener {
            checkLocationPermissionAndProceed {
                showBiometricPrompt {
                    performCheckOut(serverUrl, badgeNumber)
                }
            }
        }

        buttonLogout.setOnClickListener {
            logoutRequest(badgeNumber.toString(), serverUrl.toString())
        }

        buttonTimesheet.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_timesheetFragment)
        }

        if (!isBiometricAvailable()) {
            buttonCheckIn.isEnabled = false
            buttonCheckOut.isEnabled = false
        }
    }

    private fun logoutRequest(badgeNumber: String, serverUrl: String) {
        val json = JSONObject()
        json.put("badgeNumber", badgeNumber)
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${serverUrl}/api/logout")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()  // Not `OkHttpClient().Builder()`
            .cookieJar(cookieJar)  // Pass your CookieJar instance here
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")

                    val message = jsonResponse.optString("message", "")

                    Toast.makeText(requireContext(), "Success: $message", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(requireContext(), "Error: ", Toast.LENGTH_SHORT).show()
                    }

            }
        })
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Authentication successful!", Toast.LENGTH_SHORT).show()
                    }
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Use your fingerprint to proceed")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(requireContext())
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun checkLocationPermissionAndProceed(onPermissionGranted: () -> Unit) {
        when {
            hasLocationPermission() -> onPermissionGranted()
            else -> requestLocationPermission()
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
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun performCheckIn(serverUrl: String?, badgeNumber: String?) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                sendLocationRequest(it, serverUrl, badgeNumber, "checkIn")
            }
        }
    }

    private fun performCheckOut(serverUrl: String?, badgeNumber: String?) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                sendLocationRequest(it, serverUrl, badgeNumber, "checkOut")
            }
        }
    }

    private fun sendLocationRequest(location: Location, serverUrl: String?, badgeNumber: String?, endpoint: String) {
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("badgeNumber", badgeNumber)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${serverUrl}/api/$endpoint")
            .post(requestBody)
            .build()

        OkHttpClient.Builder().cookieJar(cookieJar).build().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                requireActivity().runOnUiThread {
                    CoroutineScope(Dispatchers.IO).launch {
                        response.use {
                            val responseBody = it.body?.string() ?: "No response from server"
                            val jsonResponse = try {
                                JSONObject(responseBody)
                            } catch (e: Exception) {
                                JSONObject().put("message", "Invalid response format")
                            }

                            val message = jsonResponse.optString("message", "Unknown error occurred")

                            // Switch to the main thread to update the UI
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Toast.makeText(requireContext(), "Success: $message", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}