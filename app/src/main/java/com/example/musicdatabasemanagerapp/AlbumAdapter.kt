package com.example.musicdatabasemanagerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlbumAdapter(private val albumList: List<Album>) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>()  {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_block, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albumList[position]
        holder.albumName.text = album.name
        val coverImage = album.getCoverImage(holder.albumCover.context)

        if(coverImage != null){
            holder.albumCover.setImageBitmap(coverImage)
        }

        holder.trackView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TrackAdapter(album.trackList)
        }

        if(album.innerListExpanded){
            holder.trackView.visibility = View.VISIBLE
        }else {
            holder.trackView.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            album.innerListExpanded = !album.innerListExpanded
            notifyItemChanged(position)

            onClickListener?.onClick(holder, position, album)
        }
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    fun setOnClickListener(listener: OnClickListener?){
        this.onClickListener = listener
    }

    interface OnClickListener {
        fun onClick(holder: ViewHolder, position: Int, album: Album){
            // Can be overridden for extra actions on album line click
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val albumName: TextView = view.findViewById(R.id.albumName)
        val albumCover: ImageView = view.findViewById(R.id.albumCover)
        val trackView: RecyclerView = view.findViewById(R.id.trackView)
    }
}