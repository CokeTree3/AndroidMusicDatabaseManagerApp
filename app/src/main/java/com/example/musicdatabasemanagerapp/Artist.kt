package com.example.musicdatabasemanagerapp

class Artist {
    var albumCount: Int = 0
    var name: String = ""
    var toBeRemoved: Boolean = false

    private var sortedAlbumList = mutableListOf<Album>()
    private var listInvalid: Boolean = true
    val albumList: List<Album> get() = if(listInvalid) sortedAlbumListAlphabetically() else sortedAlbumList

    private val albumMap = mutableMapOf<String, Album>()

    constructor(name: String){
        this.name = name
    }

    constructor(toCopyFrom: Artist, fullPath: String = "", libPath: String = ""){
        this.name = toCopyFrom.name

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

    fun printData(){
        println("$name: ")
        for(album in albumList){
            album.printData()
        }
    }
}