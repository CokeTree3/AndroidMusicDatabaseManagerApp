package com.example.musicdatabasemanagerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArtistAdapter(private val artistList: List<Artist>) : RecyclerView.Adapter<ArtistAdapter.ViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.artist_block, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artistList[position]
        holder.artistName.text = artist.name

        holder.albumBlockView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AlbumAdapter(artist.albumList)
        }
    }

    override fun getItemCount(): Int {
        return artistList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artistName: TextView = itemView.findViewById(R.id.artistName)
        val albumBlockView: RecyclerView = view.findViewById(R.id.albumView)
    }
}