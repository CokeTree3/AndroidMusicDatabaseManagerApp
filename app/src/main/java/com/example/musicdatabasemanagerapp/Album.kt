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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    constructor(json: JsonObject){
        this.name = json["name"]!!.jsonPrimitive.content

        json["tracks"]!!.jsonArray.forEach { elem ->
            val track = elem.jsonObject
            trackMap.getOrPut(track["name"]!!.jsonPrimitive.content) { Json.decodeFromJsonElement<Track>(elem) }
        }
    }

    fun isEmpty(): Boolean{
        return trackMap.isEmpty()
    }

    fun addTrack(track: Track){
        if(trackMap.containsKey(track.name)) {
            // TODO Throw error
        } else{
            trackMap[track.name] = track
        }
    }

    fun getJson(): JsonElement{
        return JsonObject(
            mapOf(
                "name" to JsonPrimitive(name),
                "tracks" to JsonArray(trackMap.map {track -> Json.encodeToJsonElement(track)})
            )
        )
    }

    fun getDiff(remoteAlbumJson: JsonObject): Album?{
        if(remoteAlbumJson["name"]!!.jsonPrimitive.content != this.name){
            return null
        }
        println("Diff for " + this.name)
        val albumDiff = Album(this.name)

        remoteAlbumJson["tracks"]!!.jsonArray.forEach { elem ->
            val trackJson = elem.jsonObject
            val locTrack = trackMap[trackJson["name"]!!.jsonPrimitive.content]

            if(locTrack != null){
                // Track already exists, skip
                println("\t" + locTrack.name + " already exists")
                if(locTrack.order != trackJson["order"]!!.jsonPrimitive.content.toInt()){
                    // TODO same track, changed order ir album
                    println("track with changed order: " + locTrack.name + " NOT IMPLEMENTED")
                }
                if(locTrack.fileName != trackJson["fileName"]!!.jsonPrimitive.content){
                    // TODO same track but changed filename
                    println("track with changed filename: " + locTrack.name + " NOT IMPLEMENTED")
                }

            } else{
                albumDiff.addTrack(Json.decodeFromJsonElement<Track>(trackJson))
            }

        }

        return albumDiff
    }

    fun mapGetOrPut(trackName: String): Track{
        return trackMap.getOrPut(trackName) { Track(trackName) }
    }

    fun mapGetOrPut(trackName: String, fileName: String, order: String): Track{
        listInvalid = true
        trackListExpanded = false
        return trackMap.getOrPut(trackName) { Track(trackName, fileName, order) }
    }

    fun mapGetOrPut(trackName: String, fileName: String, order: Int): Track{
        listInvalid = true
        trackListExpanded = false
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