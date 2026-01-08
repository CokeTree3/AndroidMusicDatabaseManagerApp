package com.example.musicdatabasemanagerapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


const val REQUEST_PERMISSION_CODE = 4723

class MainActivity : AppCompatActivity() {

    val localLibrary: Library = Library()
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

        if(checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED){
            localLibrary.buildLocal(this)
        }

        mainRecView = findViewById(R.id.mainView)

        mainRecView.layoutManager = LinearLayoutManager(this)
        mainRecView.adapter = ArtistAdapter(localLibrary.artistList)
        mainRecView.setItemViewCacheSize(20)

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