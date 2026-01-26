package com.example.musicdatabasemanagerapp

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.*
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers

import de.undercouch.bson4jackson.BsonFactory
import kotlinx.serialization.json.JsonElement
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

const val DEFAULT_PORT_NUM = 41845
const val SERVER_REQUEST_JSON = "SRQ\n"
const val SERVER_REQUEST_DATA_HEADER = "GET\n"

class Network {

    private var lastAddress: String = ""
    private var socket: io.ktor.network.sockets.Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null

    suspend fun sendData(data: String, dataLength: Int){
        val sizeArray= ByteArray(8)
        val netOrder = dataLength.reverseByteOrder()

        sizeArray[0] = (netOrder shr 0).toByte()
        sizeArray[1] = (netOrder shr 8).toByte()
        sizeArray[2] = (netOrder shr 16).toByte()
        sizeArray[3] = (netOrder shr 24).toByte()

        val dataBuf = sizeArray + data.toByteArray()
        writeChannel!!.writeFully(dataBuf)
    }

    suspend fun readHeader(): Int{

        val size1 = readChannel!!.readInt()
        val size2 = readChannel!!.readInt()

        if(size2 != 0 || size1 == 0){
            println("\nSIZE ERROR from server receive with $size1 and $size2\n")
        }
        return size1
    }

    fun convertToJson(data: ByteArray): JsonElement?{
        if(data.isEmpty()) return null

        val mapper = ObjectMapper(BsonFactory())
        val jsonString = jacksonObjectMapper().writeValueAsString(mapper.readTree(data))

        return Json.parseToJsonElement(jsonString)
    }

    suspend fun readData(readSize: Int): ByteArray{
        if(readSize < 1){
            // TODO Throw error
        }

        val dataBuf = ByteArray(readSize)
        val size = readChannel!!.readFully(dataBuf)

        return dataBuf
    }

    suspend fun openConnection(address: String){
        if(socket != null && socket?.isActive == true){
            println("already open")
            return
        }
        socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            .connect(InetSocketAddress(address, DEFAULT_PORT_NUM))

        lastAddress = address

        readChannel = socket!!.openReadChannel()
        writeChannel = socket!!.openWriteChannel(autoFlush = true)

    }



    suspend fun clientConnect(address: String): JsonElement?{
        println("conn called")

        openConnection(address)


        try{
            sendData(SERVER_REQUEST_JSON, SERVER_REQUEST_JSON.length)
        } catch (e: Exception){
            // TODO error handling
            println(e.message)
            return null
        }

        return try{
            val recvSize = readHeader()
            val recvBuf = readData(recvSize)
            convertToJson(recvBuf)
        }catch (e: Exception){

            println(e.message)
            // TODO error handling
            null
        }

    }



    suspend fun clientRequestData(requestPath: String): ByteArray?{

        if(socket == null || socket!!.isClosed){
            if(lastAddress == ""){
                // TODO return to UI thread, ask for server address
            }
            openConnection(lastAddress)
        }

        try{
            val req = SERVER_REQUEST_DATA_HEADER + requestPath
            sendData(req, req.length)
        } catch (e: Exception){
            // TODO error handling
            println("error write")
            return null
        }

        return try{
            val recvSize = readHeader()
            readData(recvSize)
        } catch (e: Exception){
            println("error read")
            // TODO error handling
            null
        }
    }

}