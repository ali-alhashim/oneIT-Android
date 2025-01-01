package com.alhashim.oneit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TimesheetAdapter(
    private val records: List<TimesheetFragment.TimesheetRecord>
) : RecyclerView.Adapter<TimesheetAdapter.TimesheetViewHolder>() {

    // ViewHolder class to hold item layout references
    class TimesheetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvCheckIn: TextView = itemView.findViewById(R.id.tvCheckIn)
        val tvCheckOut: TextView = itemView.findViewById(R.id.tvCheckOut)
    }

    // Create view for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimesheetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timesheet, parent, false)
        return TimesheetViewHolder(view)
    }

    // Bind data to each item
    override fun onBindViewHolder(holder: TimesheetViewHolder, position: Int) {
        val record = records[position]
        holder.tvDate.text = record.dayDate
        holder.tvCheckIn.text = formatTime(record.checkIn)
        holder.tvCheckOut.text = formatTime(record.checkOut)
    }

    // Helper function to format time
    private fun formatTime(time: String): String {
        return try {
            // Parse the input time (24-hour format)
            val inputFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())  // 12-hour format

            val date = inputFormat.parse(time)
            outputFormat.format(date ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
            time // Return original if parsing fails
        }
    }

    override fun getItemCount(): Int {
        return records.size
    }
}