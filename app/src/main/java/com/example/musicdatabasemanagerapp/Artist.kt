package com.example.musicdatabasemanagerapp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


class Artist {
    var name: String = ""
    var toBeRemoved: Boolean = false

    private val albumMap = mutableMapOf<String, Album>()
    private var listInvalid: Boolean = true

    private var sortedAlbumList = mutableListOf<Album>()
    val albumList: List<Album> get() {
        return if(listInvalid) sortedAlbumListAlphabetically() else sortedAlbumList
    }

    constructor(name: String){
        this.name = name
    }

    constructor(toCopyFrom: Artist, fullPath: String = "", libPath: String = ""){
        this.name = toCopyFrom.name

    }

    fun getJson(): JsonElement {
        return JsonObject(
            mapOf(
                "name" to JsonPrimitive(name),
                "albums" to JsonArray(albumMap.map { it.value.getJson() })
            )
        )
    }

    fun mapGetOrPut(albumName: String): Album{
        listInvalid = true
        return albumMap.getOrPut(albumName) { Album(albumName) }
    }

    fun sortedAlbumListAlphabetically(): List<Album>{
        if(listInvalid){
            sortedAlbumList = albumMap.values.sortedBy { it.name.lowercase() } as MutableList<Album>
            listInvalid = false
        }
        return sortedAlbumList
    }
}