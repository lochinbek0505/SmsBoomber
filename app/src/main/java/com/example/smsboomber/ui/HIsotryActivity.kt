package com.example.smsboomber.ui

import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsboomber.databinding.ActivityHisotryBinding
import com.example.smsboomber.model.Message
import com.example.smsboomber.uitilts.CatecoriesAdapter
import com.example.smsboomber.uitilts.DatabaseHandler

class HIsotryActivity : AppCompatActivity() {

    lateinit var db: DatabaseHandler
    lateinit var binding:ActivityHisotryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityHisotryBinding.inflate(layoutInflater)

        setContentView(binding.root)

        db = DatabaseHandler(this)

        val dd = db.getAllMessages()
        val adapter=CatecoriesAdapter(dd,object:CatecoriesAdapter.ItemSetOnClickListener{
            override fun onClick(data: Message) {

                sendSms(data)

            }


        })

        binding.recycler.adapter=adapter

        }


    fun sendSms(data:Message) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            // Use Telephony API for sending SMS in Android 10 and above
            val smsManager = SmsManager.getDefault()
            smsManager.sendMultipartTextMessage(
                data.number,
                null,
                smsManager.divideMessage(data.message),
                null,
                null
            )
            Toast.makeText(this, "${data.number} raqamiga SMS yuborildi.", Toast.LENGTH_LONG).show()


        } else {

            val smsManager = SmsManager.getDefault()

            smsManager.sendMultipartTextMessage(
                data.number,
                null,
                smsManager.divideMessage(data.message),
                null,
                null
            )
            Toast.makeText(this, "${data.number} raqamiga SMS yuborildi.", Toast.LENGTH_LONG).show()

        }

    }


}