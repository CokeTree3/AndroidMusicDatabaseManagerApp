package com.example.musicdatabasemanagerapp

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import java.util.concurrent.TimeUnit

class Library {
    var serverActive: Boolean = false

    private val artistMap = mutableMapOf<String, Artist>()
    private var sortedArtistList = mutableListOf<Artist>()
    private var listInvalid: Boolean = true
    val artistList: List<Artist> get() = if(listInvalid) sortedArtistListAlphabetically() else sortedArtistList

    fun addToLibrary(name: String, artist: Artist): Int{
        artistMap.getOrPut(name) { artist }
        return artistMap.size
    }

    fun addToLibrary(name: String, dirPath: String): Int {
        //artistMap.getOrPut(name) { Artist(name, dirPath) }
        return artistMap.size
    }

    fun addToLibrary(toCopyFrom: Artist, name: String): Int {
        artistMap.getOrPut(name) { Artist(toCopyFrom) }
        return artistMap.size
    }

    fun sortedArtistListAlphabetically(): List<Artist>{
        if(listInvalid){
            sortedArtistList = artistMap.values.sortedBy { it.name.lowercase() } as MutableList<Artist>
            listInvalid = false
        }
        return sortedArtistList
    }

    fun printLibContents(){
        for(artist in artistList){
            artist.printData()
        }
    }


    fun buildLocal(context: Context) {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            }else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(MediaStore.Audio.Media.DISPLAY_NAME,               // Grab media file filename, path, track name and track order from MediaStore
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TRACK)

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS).toString()           // Only consider audio files at least 5 seconds long
        )

        val sortOrder = "${MediaStore.Audio.Media.ARTIST} ASC, " +                  // Sort the data rows in (Artist Name -> Album Name -> track order) way
                "${MediaStore.Audio.Media.ALBUM} ASC, " +
                "${MediaStore.Audio.Media.TRACK} ASC"

        val query = context.contentResolver.query(                                  
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        query?.use { cursor ->
            val filenameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val relPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val orderColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            println("before loop")

            var prevArtist: Artist
            var prevAlbum: Album

            while (cursor.moveToNext()) {

                val fileName = cursor.getString(filenameColumn)
                val name = cursor.getString(nameColumn)
                val relPath = cursor.getString(relPathColumn)           // Artist and album names query from track data and rename directory if mismatched
                val trackOrderString = cursor.getString(orderColumn)

                //println("full path: $relPath$fileName")

                val artistName = relPath.substringAfter("/").substringBefore("/")
                val albumName = relPath.substringBeforeLast("/").substringAfterLast("/")

                //println("$artistName:  $albumName")

                val curArtist = artistMap.getOrPut(artistName) { Artist(artistName) }
                val curAlbum = curArtist.mapGetOrPut(albumName) // Maybe also coverBuf
                val curTrack = curAlbum.mapGetOrPut(name, fileName, trackOrderString)

                
            }
        }

        listInvalid = true
    }

}