package com.example.musicdatabasemanagerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArtistAdapter(private val artistList: List<Artist>) : RecyclerView.Adapter<ArtistAdapter.ViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.artist_block, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artistList[position]
        holder.artistName.text = artist.name

        val albumListAdapter = AlbumAdapter(artist.albumList)
        albumListAdapter.setOnClickListener(object : AlbumAdapter.OnClickListener {})

        holder.albumBlockView.layoutManager = LinearLayoutManager(holder.albumBlockView.context)
        holder.albumBlockView.adapter = albumListAdapter

        // Add animation for trackList dropdown

    }

    override fun getItemCount(): Int {
        return artistList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artistName: TextView = view.findViewById(R.id.artistName)
        val albumBlockView: RecyclerView = view.findViewById(R.id.albumView)
    }
}