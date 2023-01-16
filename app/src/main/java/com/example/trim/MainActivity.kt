package com.example.trim

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.gowtham.library.utils.CompressOption
import com.gowtham.library.utils.TrimVideo
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TrimVideo.activity(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Download/video downloader/facebook/facebook5872.mp4",
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "Download/video downloader/facebook/fbsss.mp4")
            .start(this)
    }

}