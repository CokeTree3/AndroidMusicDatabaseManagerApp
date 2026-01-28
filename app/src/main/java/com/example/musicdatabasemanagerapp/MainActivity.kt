package com.example.musicdatabasemanagerapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val syncBtn = findViewById<Button>(R.id.sync_btn)

        setSyncBtnUIState()

        syncBtn.setOnClickListener {
            syncBtnOnClick()
        }

        initLibrary()

        syncCallback()
        dataSyncCallback()

        mainRecView = findViewById(R.id.mainView)
        mainRecView.layoutManager = LinearLayoutManager(this)
        mainRecView.adapter = ArtistAdapter(mainViewModel.mainLibrary.artistList)
        mainRecView.setItemViewCacheSize(20)

    }

    private fun initLibrary(){
        if(mainViewModel.mainLibrary.libBuilt){
            return
        }

        if(checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED){
            println("no perms")
            requestPermissions()
        }

        if(checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED){
            mainViewModel.mainLibrary.buildLocal(this)
        }else {
            // TODO notification for missing required permissions
        }

    }

    private fun setSyncBtnUIState(){
        val syncBtn = findViewById<Button>(R.id.sync_btn)

        syncBtn.text = if(mainViewModel.btnActionEnabled) {
                getString(R.string.sync_stop_string)
            } else {
                getString(R.string.sync_start_string) }
    }

    private fun syncBtnOnClick(){
        println("onclick")
        val syncBtn = findViewById<Button>(R.id.sync_btn)
        mainViewModel.btnActionEnabled = !mainViewModel.btnActionEnabled
        if(mainViewModel.btnActionEnabled){
            syncBtn.text = getString(R.string.sync_stop_string)

            showAddressPopupWindow(mainViewModel::clientConnect)

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
                        } else{
                            resetSyncState("Server Connection Error")
                        }
                    }
                }

                // similar launch{} for other async actions to wait for
            }
        }
    }

    private fun implementDiff(diffLib: Library){
        if(diffLib.artistList.isEmpty()){
            println("Local data is up to date")
            resetSyncState("Local library is already up to date with the server data")
        } else{

            // Display selection popup

            mainViewModel.setSyncActive()
            mainViewModel.syncFromDiff(this)
        }
    }

    fun dataSyncCallback(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.syncActive.collect { data ->
                        if (!data) {
                            println("sync finished")
                            resetSyncState("Sync with server finished")
                        }
                    }
                }
            }
        }
    }

    private fun resetSyncState(msg: String?){
        mainViewModel.resetDiffLib()
        mainViewModel.btnActionEnabled = false
        setSyncBtnUIState()

        if(msg != null) {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showAddressPopupWindow(functionToRun: (input: String) -> Unit){
        val inputBox = EditText(this)
        inputBox.hint = "127.0.0.1"

        val testAddress = "10.0.2.2"                                                                // TODO remove for final!!!!!
        inputBox.setText(testAddress)

        val builder = AlertDialog.Builder(this)

        builder.setMessage("Input Server Address")
        builder.setView(inputBox)
        builder.setPositiveButton("Connect") { dialog, id ->
            val inputString = inputBox.text.toString()
            var valid = true

            // TODO string validity check

            if(inputString.isEmpty()) {
                valid = false
            }

            if(valid){
                functionToRun(inputString)
            }else{
                resetSyncState("Invalid Input")
            }
        }
        builder.setNegativeButton("Cancel") { dialog, id ->
            resetSyncState(null)
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()

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
    private val _syncActive = MutableStateFlow<Boolean>(false)
    val syncActive: StateFlow<Boolean> = _syncActive
    private val _diffLib = MutableSharedFlow<Library?>(1)
    val diffLibStateFlow: SharedFlow<Library?> = _diffLib

    fun clientConnect(address: String) {

        viewModelScope.launch(Dispatchers.IO) {
            if(mainLibrary.serverActive){                   // A mutex exception still remains, but the main thread could never call it twice close in time enough to cause issue
                return@launch
            }
            mainLibrary.serverActive = true
            _syncActive.value = true

            val serverLibJson = networking.clientConnect(address)

            if(serverLibJson != null && serverLibJson.jsonObject.containsKey("artists")){

                try {
                    _diffLib.emit(mainLibrary.buildDiff(serverLibJson.jsonObject["artists"]!!.jsonArray))
                }catch (e: Exception){
                    println("Malformed Server json data")
                    // TODO json diff processing errors(invalid server json likely)
                }
            } else {
                println("incorrect server data")
                mainLibrary.serverActive = false
                _syncActive.value = false
                _diffLib.emit(null)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun syncFromDiff(context: Context){
        if(_diffLib.replayCache[0] == null) {
            println("call to no diff")
            btnActionEnabled = false
            mainLibrary.serverActive = false
            return
        }

        val diffLib: Library = _diffLib.replayCache[0]!!
        _syncActive.value = true
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

                        }else{
                            // TODO error handling(notif main thread on incomplete sync)
                        }
                        break
                    }
                    break
                }
                break
            }
            _syncActive.value = false
        }
        mainLibrary.serverActive = false
        btnActionEnabled = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetDiffLib(){
        _diffLib.resetReplayCache()
    }

    fun setSyncActive(){
        if(!_syncActive.value){
            _syncActive.value = true
        }
    }

}