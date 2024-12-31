package com.alhashim.oneit

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup



class TimesheetFragment : Fragment() {




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val sharedPreferences = requireActivity().getSharedPreferences("oneIT", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "")
        val badgeNumber = sharedPreferences.getString("badgeNumber", "")
        val serverUrl = sharedPreferences.getString("serverUrl", "")

        //send http post request to serverUrl /api/timesheet
        // server response with list of timesheet record as
        // dayDate, checkIn, checkOut, totalMinutes

        return inflater.inflate(R.layout.fragment_timesheet, container, false)
    }


}