package com.example.musicdatabasemanagerapp

import android.graphics.Bitmap

class Album {
    var trackCount: Int = 0
    var name: String = ""
    var toBeRemoved: Boolean = false
    var coverImage: Bitmap? = null

    private val trackMap = mutableMapOf<String, Track>()
    private var sortedTrackList = mutableListOf<Track>()
    private var listInvalid: Boolean = true
    val trackList: List<Track> get() = if(listInvalid) sortedTrackListByOrder() else sortedTrackList


    constructor(name: String){
        this.name = name
    }

    fun mapGetOrPut(trackName: String): Track{
        return trackMap.getOrPut(trackName) { Track(trackName) }
    }

    fun mapGetOrPut(trackName: String, fileName: String, order: String): Track{
        listInvalid = true
        return trackMap.getOrPut(trackName) { Track(trackName, fileName, order) }
    }

    fun sortedTrackListByOrder(): List<Track>{
        if(listInvalid){
            sortedTrackList = trackMap.values.sortedBy { it.order } as MutableList<Track>
            listInvalid = false
        }
        return sortedTrackList
    }

    fun printData(){
        println("\t$name: ")
        for(track in trackList){
            print("\t\t")
            track.printData()
        }
    }
}