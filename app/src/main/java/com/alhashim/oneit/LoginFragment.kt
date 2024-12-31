package com.alhashim.oneit
import android.content.Context

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import okhttp3.Call

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Callback
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

import org.json.JSONObject
import java.io.IOException


class LoginFragment : Fragment() {


    private lateinit var cookieJar: MyCookieJar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cookieJar = (requireActivity().application as OneITApplication).cookieJar
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle? ): View?
    {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_login, container, false)
        val buttonLogin:Button        = view.findViewById(R.id.buttonLogin)
        val editTextServer:EditText   = view.findViewById(R.id.editTextServer)
        val editTextBadge:EditText    = view.findViewById(R.id.editTextBadge)
        val editTextPassword:EditText = view.findViewById(R.id.editTextPassword)

        val sharedPreferences = requireActivity().getSharedPreferences("oneIT", Context.MODE_PRIVATE)

        val serverUrl = sharedPreferences.getString("serverUrl", "")
        val badgeNumber = sharedPreferences.getString("badgeNumber", "")

        editTextServer.setText(serverUrl.toString())
        editTextBadge.setText(badgeNumber.toString())



        // Set click listener for login button
        buttonLogin.setOnClickListener {
            val json = JSONObject()
            json.put("badgeNumber", editTextBadge.text.toString())
            json.put("password", editTextPassword.text.toString())

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${editTextServer.text}/api/login")
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()  // Not `OkHttpClient().Builder()`
                .cookieJar(cookieJar)  // Pass your CookieJar instance here
                .build()

            // Execute HTTP call asynchronously
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Network error handling
                    activity?.runOnUiThread {
                        editTextBadge.error = "Network error: ${e.message}"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonResponse = JSONObject(responseBody ?: "{}")

                        val message = jsonResponse.optString("message", "")
                        val badgeNumber = jsonResponse.optString("badgeNumber", "")

                        if (message == "Login successful") {
                            // Store JSESSIONID from response headers after totp this in totp fragment

                            val editor = sharedPreferences.edit()





                            editor.putString("serverUrl",editTextServer.text.toString())
                            editor.putString("badgeNumber",editTextBadge.text.toString())
                            editor.apply()
                            // Proceed to TOTP screen if login is confirmed
                            activity?.runOnUiThread {
                                findNavController().navigate(R.id.action_loginFragment_to_TOTPFragment)
                            }
                        } else {
                            // Show error if message is not "Login successful"
                            activity?.runOnUiThread {
                                editTextPassword.error = "Invalid response: $message"
                            }
                        }
                    } else {
                        // Handle non-200 response codes
                        activity?.runOnUiThread {
                            editTextPassword.error = "Login failed: ${response.code}"
                        }
                    }
                }
            })
        }


        return view
    }


}