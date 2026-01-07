package com.example.musicdatabasemanagerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlbumAdapter(private val albumList: List<Album>) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_block, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albumList[position]
        holder.albumName.text = album.name
        if(album.coverImage != null) {
            holder.albumCover.setImageBitmap(album.coverImage)
        }

        /*holder.albumBlockView.apply {
            //layoutManager = LinearLayoutManager(context)
            //adapter = AlbumAdapter(artist.albums)
        }*/
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val albumName: TextView = itemView.findViewById(R.id.albumName)
        val albumCover: ImageView = view.findViewById(R.id.albumCover)
    }
}