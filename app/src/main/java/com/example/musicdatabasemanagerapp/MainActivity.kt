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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


const val REQUEST_PERMISSION_CODE = 4723

class MainActivity : AppCompatActivity() {

    val localLibrary: Library = Library()
    private lateinit var mainRecView: RecyclerView
    private var btnActionEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        findViewById<Button>(R.id.sync_btn).setOnClickListener {
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
        if(btnActionEnabled){
            syncBtn.text = getString(R.string.sync_start_string)
            // TODO halt networking process, maybe wait till a exchange finishes
        } else{
            syncBtn.text = getString(R.string.sync_stop_string)
            // TODO   popup for address,       fork thread, connect on network
        }
        btnActionEnabled = !btnActionEnabled
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
                localLibrary.buildLocal(this)
            } else {
                Toast.makeText(this, "File access required for app operation", Toast.LENGTH_LONG).show()
            }
        }
    }
}