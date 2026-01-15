package com.example.musicdatabasemanagerapp

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


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
            val printArg = Json{prettyPrint = true}
            val libJson = localLibrary.buildJson()

            /*if(libJson.containsKey("artists")){
                libJson["artists"]!!.jsonArray.forEach { println(it.jsonObject["name"]!!.jsonPrimitive.content)}
            }*/

            //println(printArg.encodeToString(libJson) )
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
                    mainViewModel.diffLib.collect { data ->
                        if (data != null) {
                            tt(data)
                        }
                    }
                }

                // similar launch{} for other async actions to wait for
            }
        }
    }

    private fun tt(diffLib: Library){
        println("in buildDiff")

        if(diffLib.artistList.isNotEmpty()) {
            diffLib.artistList[0].name
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
    private val _diffLib = MutableStateFlow<Library?>(null)
    val diffLib: StateFlow<Library?> = _diffLib

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

}