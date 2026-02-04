package com.example.musicdatabasemanagerapp

import android.app.Activity
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

enum class ClassType{
    ARTIST,
    ALBUM,
    TRACK,
}

abstract class LibraryData{
    abstract var name: String
    abstract val type: ClassType
    abstract var toBeRemoved: Boolean
    open var innerListExpanded = false

    var isChecked = true

    fun invertCheck(){
        isChecked = !isChecked
        if(!isEmpty()){
            dataList.forEach { it.invertCheck() }
        }
    }

    // TODO class parent links

    open val dataList: List<LibraryData> get() { return emptyList()}

    open fun isEmpty(): Boolean = true
}

class Library {
    var serverActive: Boolean = false
    var libBuilt: Boolean = false
    private val artistMap = mutableMapOf<String, Artist>()
    private var sortedArtistList = mutableListOf<Artist>()
    private var listInvalid: Boolean = true
    val artistList: List<Artist> get() = if(listInvalid) sortedArtistListAlphabetically() else sortedArtistList
    val delList = mutableListOf<Uri>()

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
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media._ID)

        val selection = "${MediaStore.Audio.Media.DURATION} >= ? AND " + "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS).toString()           // Only consider audio files at least 5 seconds long
        )

        val sortOrder = "${MediaStore.Audio.Media.ARTIST} ASC, " +
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
            val trackIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

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
                val trackID = cursor.getLong(trackIDColumn)

                val artistName = relPath.substringAfter("/").substringBefore("/")
                val albumName = relPath.substringBeforeLast("/").substringAfterLast("/")

                val curArtist = artistMap.getOrPut(artistName) { Artist(artistName) }
                val curAlbum = curArtist.mapGetOrPut(albumName)
                if(trackOrderString == null){
                    trackOrderString = "0"
                }

                val curTrack = curAlbum.mapGetOrPut(name, fileName, trackOrderString, trackID)

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

        val curTrack = curAlbum.mapGetOrPut(trackName?: "", fileName, trackOrder, 0)

        // TODO Downloaded tracks are missing their file ID and cant be removed before restart

        if(curAlbum.coverImage == Uri.EMPTY) {
            // TODO New albums wont have cover image loaded until app restart
        }

    }

    fun removeLocalArtist(context: Activity, name: String){
        val locArtist = artistMap[name]

        if(locArtist == null){
            // TODO throw error
            return
        }

        for(album in locArtist.albumList){
            locArtist.removeLocalAlbum(context, album.name)?.let { delList.addAll(it) }
        }
        // TODO empty directory remains
    }

    fun removeLocalAlbum(context: Activity, name: String, artistName: String){
        artistMap[artistName]?.removeLocalAlbum(context, name)?.let { delList.addAll(it) }
    }
    fun removeLocalTrack(context: Activity, name: String, artistName: String, albumName: String){
        artistMap[artistName]?.get(albumName)?.removeLocalTrack(context, name)?.let { delList.add(it) }
    }

    fun execDeleteReq(context: Activity){
        if(delList.isEmpty()){
            return
        }
        val resolver = context.contentResolver

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try{
                val deleteRequest = MediaStore.createDeleteRequest(
                    resolver,
                    delList
                )

                context.startIntentSenderForResult(
                    deleteRequest.intentSender,
                    REQUEST_PERMISSION_CODE,
                    null,
                    0,
                    0,
                    0
                )
            }catch (e: Exception){
                println(e.message)

            }
        } else{
            println("Pre Android R")
            // TODO different deletion implementation
        }


    }

    fun buildJson(): JsonObject{
        return JsonObject(mapOf("artists" to JsonArray(artistMap.map {it.value.getJson()})))
    }

    fun buildDiff(remoteLibJson: JsonArray): Library{

        val diffLib = Library()
        val mapList = mutableListOf<String>()

        remoteLibJson.forEach { elem ->
            val artistJson = elem.jsonObject
            val locArtist = artistMap[artistJson["name"]!!.jsonPrimitive.content]
            if(locArtist != null){
                val artistDiff = locArtist.getDiff(artistJson)
                if(artistDiff != null && !artistDiff.isEmpty()){
                    diffLib.addArtist(artistDiff)
                }
                mapList.add(locArtist.name)
            }else{
                diffLib.addArtist(Artist(artistJson))
            }
        }

        for(key in artistMap.keys){
            if(!mapList.contains(key)){
                diffLib.addArtist(Artist(key))
                diffLib.get(key)!!.toBeRemoved = true
            }
        }

        diffLib.libBuilt = true
        return diffLib
    }

}