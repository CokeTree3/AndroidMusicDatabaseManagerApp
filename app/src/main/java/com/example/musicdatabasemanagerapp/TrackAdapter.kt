package com.example.musicdatabasemanagerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(private val trackList: List<Track>) : RecyclerView.Adapter<TrackAdapter.ViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_block, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = trackList[position]
        holder.trackName.text = track.name

        val nrString = (position + 1).toString() + ".  "
        holder.trackNr.text = nrString
    }

    override fun getItemCount(): Int {
        return trackList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val trackName: TextView = view.findViewById(R.id.trackName)
        val trackNr: TextView = view.findViewById(R.id.trackNr)
    }
}