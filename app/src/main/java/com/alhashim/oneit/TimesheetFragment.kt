package com.alhashim.oneit

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class TimesheetFragment : Fragment() {
    private lateinit var cookieJar: MyCookieJar

    data class TimesheetRecord(
        val dayDate: String,
        val checkIn: String,
        val checkOut: String,
        val totalMinutes: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cookieJar = (requireActivity().application as OneITApplication).cookieJar
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timesheet, container, false)

        val sharedPreferences = requireActivity().getSharedPreferences("oneIT", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "")
        val badgeNumber = sharedPreferences.getString("badgeNumber", "")
        val serverUrl = sharedPreferences.getString("serverUrl", "")

        fetchTimesheet(serverUrl, badgeNumber)

        return view
    }

    private fun fetchTimesheet(serverUrl: String?, badgeNumber: String?) {
        println("Fetching timesheet records...")

        val json = JSONObject().apply {
            put("badgeNumber", badgeNumber)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${serverUrl}/api/timesheet")
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
                        "Failed to fetch timesheet: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                println("Timesheet response: $responseBody")

                requireActivity().runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val recordsArray = JSONArray(responseBody)
                            val timesheetRecords = mutableListOf<TimesheetRecord>()

                            for (i in 0 until recordsArray.length()) {
                                val record = recordsArray.getJSONObject(i)
                                timesheetRecords.add(
                                    TimesheetRecord(
                                        dayDate = record.getString("dayDate"),
                                        checkIn = record.getString("checkIn"),
                                        checkOut = record.getString("checkOut"),
                                        totalMinutes = record.getInt("totalMinutes")
                                    )
                                )
                            }
                            displayTimesheet(view, timesheetRecords)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(
                                requireContext(),
                                "Error parsing timesheet data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to fetch timesheet: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun displayTimesheet(view: View?, records: List<TimesheetRecord>) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TimesheetAdapter(records)
            setHasFixedSize(true)
        }
    }
}