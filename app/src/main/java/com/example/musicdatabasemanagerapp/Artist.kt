package com.example.musicdatabasemanagerapp

import android.app.Activity
import android.net.Uri
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


class Artist : LibraryData {
    override var name: String = ""
    override var toBeRemoved: Boolean = false
    override val type = ClassType.ARTIST
    override var innerListExpanded = false

    private val albumMap = mutableMapOf<String, Album>()
    private var listInvalid: Boolean = true

    private var sortedAlbumList = mutableListOf<Album>()
    val albumList: List<Album> get() {
        return if(listInvalid) sortedAlbumListAlphabetically() else sortedAlbumList
    }

    override val dataList: List<Album> get() { return albumList}

    constructor(name: String){
        this.name = name
    }

    constructor(json: JsonObject){
        this.name = json["name"]!!.jsonPrimitive.content

        json["albums"]!!.jsonArray.forEach { elem ->
            val albumJson = elem.jsonObject
            albumMap.getOrPut(albumJson["name"]!!.jsonPrimitive.content) { Album(albumJson) }
        }
    }

    fun get(name: String): Album?{
        return albumMap[name]
    }

    override fun isEmpty(): Boolean{
        return albumMap.isEmpty()
    }

    fun addAlbum(album: Album){
        if(albumMap.containsKey(album.name)) {
            // TODO Throw error
        } else{
            albumMap[album.name] = album
        }
    }

    fun getJson(): JsonElement {
        return JsonObject(
            mapOf(
                "name" to JsonPrimitive(name),
                "albums" to JsonArray(albumMap.map { it.value.getJson() })
            )
        )
    }

    fun getDiff(remoteArtistJson: JsonObject): Artist?{
        if(remoteArtistJson["name"]!!.jsonPrimitive.content != this.name){
            return null
        }

        val artistDiff = Artist(this.name)
        val mapList = mutableListOf<String>()

        remoteArtistJson["albums"]!!.jsonArray.forEach { elem ->

            val albumJson = elem.jsonObject
            val locAlbum = albumMap[albumJson["name"]!!.jsonPrimitive.content]

            if (locAlbum != null) {
                val albumDiff = locAlbum.getDiff(albumJson)
                if (albumDiff != null && !albumDiff.isEmpty()) {
                    artistDiff.addAlbum(albumDiff)
                }
                mapList.add(locAlbum.name)
            } else {
                artistDiff.addAlbum(Album(albumJson))
            }
        }

        for(key in albumMap.keys) {
            if (!mapList.contains(key)) {
                artistDiff.addAlbum(Album(key))
                artistDiff.get(key)!!.toBeRemoved = true
            }
        }
        return artistDiff
    }

    fun removeLocalAlbum(context: Activity, name: String): List<Uri>?{
        val locAlbum = albumMap[name]
        val delList = mutableListOf<Uri>()
        if(locAlbum == null){
            // TODO throw error
            return null
        }

        for(track in locAlbum.trackList){

            locAlbum.removeLocalTrack(context, track.name)?.let { delList.add(it) }
        }
        // TODO empty directory remains
        return delList
    }

    fun mapGetOrPut(albumName: String): Album{
        listInvalid = true
        return albumMap.getOrPut(albumName) { Album(albumName) }
    }

    fun sortedAlbumListAlphabetically(): List<Album>{
        if(albumMap.isEmpty()){
            return emptyList()
        }
        if(listInvalid){
            sortedAlbumList = albumMap.values.sortedBy { it.name.lowercase() } as MutableList<Album>
            listInvalid = false
        }
        return sortedAlbumList
    }
}