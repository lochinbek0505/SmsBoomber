package com.example.smsboomber

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsboomber.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import java.util.Calendar
import java.util.UUID


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "MyPrefs"
    private val KEY_NAME = "username"
    private val KEY_NAME2 = "passport"
    private val KEY_NAME3 = "check"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val savedUsername = sharedPreferences.getString(KEY_NAME3, "")

        binding.tvHandbook.setOnClickListener {

            startActivity(Intent(this, MainActivity2::class.java))

        }

        binding.tvLink.setOnClickListener {

            val telegram = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sevdo2004"))
//            telegram.setPackage("org.telegram.messenger")
            startActivity(telegram)
        }
        if (savedUsername == "check") {

            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()

        }


        binding.btnLogin.setOnClickListener {


            if (!(binding.etNumber.text.isNullOrEmpty() and binding.etPassport.text.isNullOrEmpty())) {


                postDataUsingRetrofit(
                    this,
                    binding.etNumber.text.toString(),
                    binding.etPassport.text.toString()
                )


            } else {

                Toast.makeText(this, "Iltimos maydonlarni to'liq to'ldiring", Toast.LENGTH_LONG)
                    .show()


            }

//        } else {
//
//
//            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
//
//        }


        }
    }

    private fun saveUsername(username: String, password: String, check: String) {

        val editor = sharedPreferences.edit()

        editor.putString(KEY_NAME, username)
        editor.putString(KEY_NAME2, password)
        editor.putString(KEY_NAME3, check)


        editor.apply()


    }

    fun getDeviceUUID(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var uuid = sharedPreferences.getString("uuid", "")
        if (uuid.isNullOrEmpty()) {
            uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("uuid", uuid).apply()
        }
        return uuid
    }


    private fun postDataUsingRetrofit(

        context: Context,
        number: String, password: String
    ) {
        var url = "https://smsboomber.pythonanywhere.com/api/v1/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val dataModel = DataModel(getDeviceUUID(context),number, password, getCurrentHour().toString(),"login")
        val call: Call<login_respons?>? = retrofitAPI.login(dataModel)

        call!!.enqueue(object : Callback<login_respons?> {
            override fun onResponse(
                call: Call<login_respons?>?,
                response: Response<login_respons?>
            ) {


//                Toast.makeText(context,response.body()!!.status.toString(),Toast.LENGTH_LONG).show()


                if (response.body()!!.status.equals("true")) {
                    saveUsername(number, password, "check")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                }
                else if(response.body()!!.status.equals("double")){

                    Toast.makeText(context, "2 ta qurilmadan kirish.", Toast.LENGTH_LONG).show()


                }
                else {

                    Toast.makeText(context, "Login yoki Parol xato", Toast.LENGTH_LONG).show()


                }

            }

            override fun onFailure(call: Call<login_respons?>?, t: Throwable) {

                Toast.makeText(context, "${t.message}", Toast.LENGTH_LONG).show()


            }
        })


    }

    fun getCurrentHour(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android versions Oreo (API level 26) and above
            val currentTime = LocalTime.now()
            currentTime.hour // 24-hour format
        } else {
            // For versions before Android Oreo
            val calendar = Calendar.getInstance()
            calendar.get(Calendar.HOUR_OF_DAY) // 24-hour format
        }
    }


}