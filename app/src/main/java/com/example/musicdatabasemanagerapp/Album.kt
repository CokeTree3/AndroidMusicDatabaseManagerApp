package com.example.musicdatabasemanagerapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.io.IOException

class Album {
    var name: String = ""
    var toBeRemoved: Boolean = false
    var coverImage: Uri = Uri.EMPTY
    var trackListExpanded = false

    private val trackMap = mutableMapOf<String, Track>()
    private var listInvalid: Boolean = true
    private var sortedTrackList = mutableListOf<Track>()

    val trackList: List<Track> get() {
        return if(listInvalid) sortedTrackListByOrder() else sortedTrackList
    }


    constructor(name: String){
        this.name = name
    }

    fun getJson(): JsonElement{
        return JsonObject(
            mapOf(
                "name" to JsonPrimitive(name),
                "tracks" to JsonArray(trackMap.map {track -> Json.encodeToJsonElement(track)})
            )
        )
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

    fun getCoverImage(context: Context): Bitmap? {
        try{
            if(coverImage != Uri.EMPTY) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return context.contentResolver.loadThumbnail(coverImage, Size(48, 48), null)
                }

                return null
            }
        } catch (e: IOException){
            println("Missing Cover for $name")
            return null
        }

        return null
    }
}