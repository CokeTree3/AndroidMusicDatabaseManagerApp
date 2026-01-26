package com.example.musicdatabasemanagerapp

import android.annotation.SuppressLint
import androidx.core.text.isDigitsOnly
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@SuppressLint("UnsafeOptInUsageError")
@Serializable
class Track {
    var name: String = ""
    var order: Int = 0
    var fileName: String = ""
    @Transient var toBeRemoved: Boolean = false

    constructor(name: String){
        this.name = name
    }

    constructor(name: String, fileName: String, order: Int){
        this.name = name
        this.fileName = fileName
        this.order = order
    }

    constructor(name: String, fileName: String, order: String){
        this.name = name
        this.fileName = fileName
        if(order.isDigitsOnly()){
            if(order.length == 4){
                this.order = order.removeRange(0,1).toInt()
            }else {
                this.order = order.toInt()
            }
        }
    }

    /*constructor(json: String){
        val jsonData = Json.decodeFromString<TrackJSON>(json)

        this.name = jsonData.name
        this.order = jsonData.order
        this.fileName = jsonData.fileName
    }*/

    /*fun getJSON(): String{
        return Json.encodeToString(TrackJSON(name, fileName, order))
    }

    @Serializable
    data class TrackJSON(val name: String, val fileName: String, val order: Int)*/
}