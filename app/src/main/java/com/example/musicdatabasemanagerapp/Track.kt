package com.example.musicdatabasemanagerapp

import android.annotation.SuppressLint
import androidx.core.text.isDigitsOnly
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@SuppressLint("UnsafeOptInUsageError")
@Serializable
class Track : LibraryData {
    override var name: String = ""
    var order: Int = 0
    var fileName: String = ""
    @Transient override var toBeRemoved: Boolean = false
    @Transient override val type = ClassType.TRACK

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
}