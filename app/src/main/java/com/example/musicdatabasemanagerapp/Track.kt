package com.example.musicdatabasemanagerapp

import androidx.core.text.isDigitsOnly

class Track {
    var name: String = ""
    var order: Int = 0
    var fileName: String = ""
    var toBeRemoved: Boolean = false

    constructor(name: String){
        this.name = name
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

    fun printData(){
        println("$order - $name")
    }
}