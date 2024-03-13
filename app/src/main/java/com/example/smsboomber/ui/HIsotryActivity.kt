package com.example.smsboomber.ui

import com.example.smsboomber.uitilts.DatabaseHandler
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.smsboomber.R

class HIsotryActivity : AppCompatActivity() {

    lateinit var db: DatabaseHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hisotry)

        db = DatabaseHandler(this)

        val dd = db.getAllMessages()
        for (message in dd) {

            Log.e("HIsotryActivity",message.toString())

        }


        }
//        Log.e{"HIsotryActivity",dd.toString}

}