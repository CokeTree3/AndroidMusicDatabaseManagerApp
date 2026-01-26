package com.example.musicdatabasemanagerapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject


const val REQUEST_PERMISSION_CODE = 4723

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var mainRecView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val localLibrary: Library = mainViewModel.mainLibrary

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if(checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED){
            println("Granted");
        }else{
            println("no perms")
            requestPermissions()
        }
        val textView: TextView = findViewById<TextView>(R.id.lib_title)

        textView.setOnClickListener {
            Toast.makeText(this, "test msg\n this is very long, does the text wrapping work if needed at all", Toast.LENGTH_SHORT).show()
        }

        val syncBtn = findViewById<Button>(R.id.sync_btn)

        syncBtn.text = if(mainViewModel.btnActionEnabled) {
                getString(R.string.sync_stop_string)
            }else {
                getString(R.string.sync_start_string) }

        syncCallback()
        syncBtn.setOnClickListener {
            syncBtnOnClick()
        }

        if(checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED){
            localLibrary.buildLocal(this)
        }

        mainRecView = findViewById(R.id.mainView)

        mainRecView.layoutManager = LinearLayoutManager(this)
        mainRecView.adapter = ArtistAdapter(localLibrary.artistList)
        mainRecView.setItemViewCacheSize(20)

    }

    private fun syncBtnOnClick(){
        val syncBtn = findViewById<Button>(R.id.sync_btn)
        mainViewModel.btnActionEnabled = !mainViewModel.btnActionEnabled
        if(mainViewModel.btnActionEnabled){
            syncBtn.text = getString(R.string.sync_stop_string)
            // TODO   popup for address,       fork thread, connect on network
            mainViewModel.clientConnect("10.0.2.2")

        } else{
            syncBtn.text = getString(R.string.sync_start_string)
            // TODO halt networking process, maybe wait till a exchange finished
        }
    }

    private fun syncCallback(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    mainViewModel.diffLibStateFlow.collect { data ->
                        if (data != null) {
                            implementDiff(data)
                        }
                    }
                }

                // similar launch{} for other async actions to wait for
            }
        }
    }

    private fun implementDiff(diffLib: Library){
        println("diff Built")

        // Display selection popup

        dataSyncCallback()
        mainViewModel.syncFromDiff(this)

        // returns immediately, do nothing here

    }

    fun dataSyncCallback(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.syncActive.collect { data ->
                        if (!data) {
                            println("\nsync finished\n")
                            // syncBtnOnClick()
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO), REQUEST_PERMISSION_CODE)
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val localLibrary: Library = mainViewModel.mainLibrary
                localLibrary.buildLocal(this)
            } else {
                Toast.makeText(this, "File access required for app operation", Toast.LENGTH_LONG).show()
            }
        }
    }
}


class MainViewModel: ViewModel() {
    val mainLibrary = Library()
    var btnActionEnabled = false
    val networking = Network()
    val syncActive = MutableStateFlow<Boolean>(false)
    private val _diffLib = MutableStateFlow<Library?>(null)
    val diffLibStateFlow: StateFlow<Library?> = _diffLib

    fun clientConnect(address: String) {

        viewModelScope.launch(Dispatchers.IO) {
            if(mainLibrary.serverActive){                   // A mutex exception still remains, but the main thread could never call it twice close in time enough to cause issue
                return@launch
            }
            mainLibrary.serverActive = true

            val serverLibJson = networking.clientConnect(address)

            if(serverLibJson != null && serverLibJson.jsonObject.containsKey("artists")){

                try {
                    _diffLib.value = mainLibrary.buildDiff(serverLibJson.jsonObject["artists"]!!.jsonArray)
                }catch (e: Exception){
                    println("Malformed Server json data")
                    // TODO json diff processing errors(invalid server json likely)
                }
            } else {
                println("incorrect server data")
                return@launch
            }

        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun syncFromDiff(context: Context){
        if(diffLibStateFlow.value == null) {
            println("call to empty diff")
            btnActionEnabled = false
            return
        }

        val diffLib: Library = diffLibStateFlow.value!!
        syncActive.value = true

        viewModelScope.launch(Dispatchers.IO) {
            for(artistDiff in diffLib.artistList){
                for(albumDiff in artistDiff.albumList){
                    for(track in albumDiff.trackList){
                        val reqPath = artistDiff.name + "/" + albumDiff.name + "/" + track.fileName
                        println(reqPath)

                        val trackData = networking.clientRequestData(reqPath)

                        if(trackData != null){
                            if(trackData[0] == 0x49.toByte() && trackData[1] == 0x44.toByte() && trackData[2] == 0x33.toByte()){
                                 println("MP3 file header (ID3)")
                            }else if(trackData[0] == 0x66.toByte() && trackData[1] == 0x4c.toByte() && trackData[2] == 0x61.toByte() && trackData[3] == 0x43.toByte()){
                                println("FLAC file header (fLaC)")
                            } else {
                                println("Unsupported or corrupt file download")
                                continue
                            }

                            try{
                                mainLibrary.insertTrack(context, trackData, track.fileName, albumDiff.name, artistDiff.name, track.order)

                            }catch (e: Exception){
                                println("Error downloading audio track: $reqPath")
                            }

                        }
                        break
                    }
                    break
                }
                break

                // request data from server for each track, add to mediaStore via mainLib, update mainLib(possibly via recalled buildLocal()(slow as MS queried again)




            }
            syncActive.value = false
        }
        btnActionEnabled = false
    }

}