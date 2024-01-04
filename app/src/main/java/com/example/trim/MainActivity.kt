package com.example.trim

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.gowtham.utils.TrimVideo
import com.gowtham.utils.dataMerge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        TrimVideo.activity(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Download/hello.mp4",
//            Environment.getExternalStorageDirectory().absolutePath + File.separator + "Download/video downloader").start(this)
//        val result = dataMerge().execute(context,listofvideos,outputfilepathwithextension)
//        if (result == 0) { }
//        else { }

    }


}