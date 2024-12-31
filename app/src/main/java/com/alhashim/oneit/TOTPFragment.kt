package com.alhashim.oneit

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class TOTPFragment : Fragment() {

    private lateinit var cookieJar: MyCookieJar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cookieJar = (requireActivity().application as OneITApplication).cookieJar
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_t_o_t_p, container, false)
        val editTextTOTP:EditText = view.findViewById(R.id.editTextTOTP)
        val buttonVerifyTOTP:Button = view.findViewById(R.id.buttonVerifyTOTP)

        buttonVerifyTOTP.setOnClickListener {
            // Get SharedPreferences
            val sharedPreferences = requireActivity().getSharedPreferences("oneIT", Context.MODE_PRIVATE)

            val serverUrl = sharedPreferences.getString("serverUrl", "")
            val badgeNumber = sharedPreferences.getString("badgeNumber", "")

            println(" http post request to $serverUrl for $badgeNumber")

            // Prepare JSON body for the TOTP verification request
            val json = JSONObject()
            json.put("totpCode", editTextTOTP.text.toString())
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            // Build the HTTP request
            val request = Request.Builder()
                .url("${serverUrl}/api/verify-totp")
                .post(requestBody)
                .build()

            // Execute the HTTP request asynchronously
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    // Read the response body once and store it
                    val responseBody = response.body?.string()
                    println("Totp response.message= $responseBody")

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val message = jsonResponse.optString("message", "")
                            val name = jsonResponse.optString("name", "")
                            val badgeNumber = jsonResponse.optString("badgeNumber", "")

                            requireActivity().runOnUiThread {
                                if (message == "OTP verified successfully") {
                                    // Save both name and badgeNumber in SharedPreferences
                                    val editor = sharedPreferences.edit()
                                    editor.putString("userName", name)
                                    editor.putString("badgeNumber", badgeNumber)
                                    editor.apply()

                                    Toast.makeText(requireContext(), "Welcome, $name", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_TOTPFragment_to_dashboardFragment)
                                } else {
                                    Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: JSONException) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
        return view
    }


}