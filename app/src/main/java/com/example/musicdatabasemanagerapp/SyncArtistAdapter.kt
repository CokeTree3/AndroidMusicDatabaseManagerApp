package com.example.musicdatabasemanagerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SyncArtistAdapter(private val dataList: List<LibraryData>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            ClassType.ARTIST.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.sync_artist_line, parent, false)
                return ViewHolder(view)
            }
            ClassType.ALBUM.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.sync_album_line, parent, false)
                return ViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.sync_track_line, parent, false)
                return ViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return dataList[position].type.ordinal
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val element = dataList[position]
        holder as ViewHolder


        if(element.toBeRemoved){
            val dispText = element.name + "  !(To Be Removed)!"
            holder.name.text = dispText
        }else{
            holder.name.text = element.name
        }

        if(!element.isEmpty()){
            holder.expandIcon.visibility = View.VISIBLE
            holder.expandIcon.isGone = false
        }

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = element.isChecked


        holder.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            element.invertCheck()
            notifyItemChanged(position)

            // TODO add upward updates for parent element

        }

        if(element.type != ClassType.TRACK) {

            holder.subView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = SyncArtistAdapter(element.dataList)
            }

            if (element.innerListExpanded) {
                holder.subView.visibility = View.VISIBLE
                holder.expandIcon.setImageResource(R.drawable.twotone_arrow_drop_down_24)
            } else {
                holder.subView.visibility = View.GONE
                holder.expandIcon.setImageResource(R.drawable.twotone_arrow_drop_up_24)
            }

            holder.itemView.setOnClickListener {
                element.innerListExpanded = !element.innerListExpanded
                notifyItemChanged(position)

                onClickListener?.onClick(holder, position, element)
            }
        }

        // TODO Add animation for list dropdown

    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setOnClickListener(listener: OnClickListener?){
        this.onClickListener = listener
    }

    interface OnClickListener {
        fun onClick(holder: ViewHolder, position: Int, element: LibraryData){
            println(position)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.sync_name)
        val expandIcon: ImageView = view.findViewById(R.id.expand_icon)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val subView: RecyclerView = view.findViewById(R.id.sync_dropdown_rec_view)

    }
}