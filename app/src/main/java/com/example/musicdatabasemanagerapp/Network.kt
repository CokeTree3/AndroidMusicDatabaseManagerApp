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
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

const val DEFAULT_PORT_NUM = 41845
const val SERVER_REQUEST_DATA = "SRQ\n"

class Network {

    //var socket: Socket? = null

    suspend fun sendData(socketOut: ByteWriteChannel, data: String, dataLength: Int){
        val sizeArray= ByteArray(8)
        val netOrder = dataLength.reverseByteOrder()

        sizeArray[0] = (netOrder shr 0).toByte()
        sizeArray[1] = (netOrder shr 8).toByte()
        sizeArray[2] = (netOrder shr 16).toByte()
        sizeArray[3] = (netOrder shr 24).toByte()

        val dataBuf = sizeArray + data.toByteArray()
        socketOut.writeFully(dataBuf)
    }

    suspend fun readHeader(socketIn: ByteReadChannel): Int{

        val size1 = socketIn.readInt()
        val size2 = socketIn.readInt()

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

    suspend fun readData(socketIn: ByteReadChannel, readSize: Int): ByteArray{
        if(readSize < 1){
            // TODO Throw error
        }

        val dataBuf = ByteArray(readSize)
        val size = socketIn.readAvailable(dataBuf)

        return dataBuf
    }



    suspend fun clientConnect(address: String): JsonElement?{
        println("conn called")
        val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            .connect(InetSocketAddress(address, DEFAULT_PORT_NUM))

        println("Connected to server")
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        try{
            sendData(output, SERVER_REQUEST_DATA, SERVER_REQUEST_DATA.length)
        } catch (e: Exception){
            // TODO error handling
        }

        return try{
            val recvSize = readHeader(input)
            val recvBuf = readData(input, recvSize)
            convertToJson(recvBuf)
        }catch (e: Exception){
            // TODO error handling
            null
        }
        
    }

}