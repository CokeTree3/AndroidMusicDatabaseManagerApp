package com.example.musicdatabasemanagerapp

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import kotlinx.serialization.json.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class Library {
    var serverActive: Boolean = false
    var libBuilt: Boolean = false
    private val artistMap = mutableMapOf<String, Artist>()
    private var sortedArtistList = mutableListOf<Artist>()
    private var listInvalid: Boolean = true
    val artistList: List<Artist> get() = if(listInvalid) sortedArtistListAlphabetically() else sortedArtistList

    fun get(name: String): Artist?{
        return artistMap[name]
    }

    fun sortedArtistListAlphabetically(): List<Artist>{
        if(listInvalid){
            sortedArtistList = artistMap.values.sortedBy { it.name.lowercase() } as MutableList<Artist>
            listInvalid = false
        }
        return sortedArtistList
    }

    fun addArtist(artist: Artist){
        if(artistMap.containsKey(artist.name)) {
            // Throw error
        } else{
            artistMap[artist.name] = artist
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
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.ALBUM_ID)

        val selection = "${MediaStore.Audio.Media.DURATION} >= ? AND " + "${MediaStore.Audio.Media.IS_MUSIC} != 0"
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

            val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {

                val fileName = cursor.getString(filenameColumn)
                val name = cursor.getString(nameColumn)
                val relPath = cursor.getString(relPathColumn)           // Artist and album names query from track data and rename directory if mismatched

                if(!relPath.startsWith("Music/")){
                    continue
                }

                var trackOrderString = cursor.getStringOrNull(orderColumn)
                val albumID = cursor.getLong(albumIDColumn)

                val artistName = relPath.substringAfter("/").substringBefore("/")
                val albumName = relPath.substringBeforeLast("/").substringAfterLast("/")

                val curArtist = artistMap.getOrPut(artistName) { Artist(artistName) }
                val curAlbum = curArtist.mapGetOrPut(albumName)
                if(trackOrderString == null){
                    trackOrderString = "0"
                }

                val curTrack = curAlbum.mapGetOrPut(name, fileName, trackOrderString)

                if(curAlbum.coverImage == Uri.EMPTY) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            curAlbum.coverImage = ContentUris.withAppendedId(
                                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                albumID
                            )
                        } else {
                            // TODO Need to query Audio.Album table for AlbumID row
                        }

                    } catch (e: IOException) {
                        curAlbum.coverImage = Uri.EMPTY
                    }

                }
            }
        }
        listInvalid = true
        libBuilt = true
    }

    fun insertTrack(context: Context, trackFileBuf: ByteArray, fileName: String, albumName: String, artistName: String, trackOrder: Int){

        // TODO Each track is inserted individually, might cause slowdowns, test batch insertion

        val resolver = context.contentResolver

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            }else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val relPath = Environment.DIRECTORY_MUSIC + "/$artistName/$albumName/"

        val trackData = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relPath)          // TODO if < Build.VERSION_CODES.Q, rel path is ignored, so tracks wont be added to subfolders
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val songContentUri = resolver.insert(collection, trackData)

        resolver.openOutputStream(songContentUri!!).use { stream ->
            stream!!.write(trackFileBuf)
            stream.flush()
        }

        trackData.clear()
        trackData.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(songContentUri, trackData, null, null)

        val trackName = MediaMetadataRetriever().let { retriever ->
            try{
                retriever.setDataSource(context, songContentUri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            }finally {
                retriever.release()
            }
        }

        val curArtist = artistMap.getOrPut(artistName) { Artist(artistName) }
        val curAlbum = curArtist.mapGetOrPut(albumName)

        val curTrack = curAlbum.mapGetOrPut(trackName?: "", fileName, trackOrder)

        if(curAlbum.coverImage == Uri.EMPTY) {
            // TODO New albums wont have cover image loaded until app restart
        }

    }

    fun buildJson(): JsonObject{
        return JsonObject(mapOf("artists" to JsonArray(artistMap.map {it.value.getJson()})))
    }

    fun buildDiff(remoteLibJson: JsonArray): Library{

        val diffLib = Library()

        remoteLibJson.forEach { elem ->
            val artistJson = elem.jsonObject
            val locArtist = artistMap[artistJson["name"]!!.jsonPrimitive.content]
            if(locArtist != null){
                val artistDiff = locArtist.getDiff(artistJson)
                if(artistDiff != null && !artistDiff.isEmpty()){
                    diffLib.addArtist(artistDiff)
                }
                //println("changed artist contents for " + artistJson["name"]!!.jsonPrimitive.content)
            }else{
                diffLib.addArtist(Artist(artistJson))
            }
        }

        diffLib.libBuilt = true;
        return diffLib
    }

}