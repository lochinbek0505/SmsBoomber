package com.example.smsboomber

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smsboomber.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import java.util.Calendar

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private val PREF_NAME = "MyPrefs"
    private val KEY_NAME = "username"
    private val KEY_NAME2 = "passport"
    private val KEY_NAME3 = "check"
    val storageRef = Firebase.storage.reference;
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var dialog: AlertDialog
    private lateinit var dialogSMS: AlertDialog
    private val SMS_PERMISSION_REQUEST_CODE = 101
    var savedUsername = ""
    var savedPassport = ""
    var saveduid=""
    var uploadFile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        savedUsername = sharedPreferences.getString(KEY_NAME, "").toString()
        savedPassport = sharedPreferences.getString(KEY_NAME2, "").toString()
        saveduid=sharedPreferences.getString("uuid","").toString()

        postDataUsingRetrofit(this@MainActivity, savedUsername, savedPassport)
//        binding.tvLeftDays.text=saveduid
        if (postDataUsingRetrofit(this@MainActivity, savedUsername, savedPassport)) {

            Toast.makeText(this, "Obuna muddati tugagan ", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))

            finish()
            clearUsername()


        }

        binding.ivLogout.setOnClickListener {

            logout1(this, savedUsername)

        }


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }


        if (!checkForInternet(this)) {
            checker()
        }


        binding.sendFile.setOnClickListener {

            val galleryIntent = Intent(Intent.ACTION_PICK)

            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//
            galleryIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            startActivityForResult(galleryIntent, 1);

            imagePickerActivityResult.launch(galleryIntent)


        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending SMS
//                sendSMS()
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
            }
        }
    }

    fun logout() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("DIQQAT!")
        builder.setMessage("Rostan ham hisobdan chiqmoqchimisiz")
        builder.setCancelable(true)
//builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->

            startActivity(Intent(this, LoginActivity::class.java))

            finish()
            clearUsername()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which ->

        }


        builder.show()

    }

    private fun clearUsername() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_NAME)
        editor.remove(KEY_NAME3)
        editor.remove(KEY_NAME2)
        editor.apply()
    }

    private var imagePickerActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result != null) {

                setProgressDialog()

                val imageUri: Uri? = result.data?.data

                val sd = getFileName(applicationContext, imageUri!!)
                val name = "$sd _ ${System.currentTimeMillis()}"
                val uploadTask = storageRef.child("file/$name").putFile(imageUri!!)

                uploadTask.addOnSuccessListener {

                    Toast.makeText(this, "Fayl yuklandi!!! ", Toast.LENGTH_LONG).show()
                    storageRef.child("file/$name").downloadUrl.addOnSuccessListener {


                        postDataUsingRetrofit(this, savedUsername, savedPassport, it.toString())


                    }

                    dialog.cancel()
                    uploadFile = true

                }.addOnFailureListener {
                    Log.e("Firebase", "Image Upload fail")
                }
            }
        }
//    getColumnIndex(OpenableColumns.DISPLAY_NAME)

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }


    fun postDataUsingRetrofit(
        context: Context,
        number: String, password: String,
    ): Boolean {

        var check = false
        var url = "https://smsboomber.pythonanywhere.com/api/v1/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val dataModel = DataModel(saveduid,number, password, getCurrentHour().toString(),"main")
        val call: Call<login_respons?>? = retrofitAPI.login(dataModel)

        call!!.enqueue(object : Callback<login_respons?> {
            override fun onResponse(
                call: Call<login_respons?>?,
                response: Response<login_respons?>
            ) {


                binding.tvLeftDays.text = "Limit tugashiga  ${response.body()!!.days} kun qoldi."

                if (response.body()!!.status.equals("timeout")) {

                    Toast.makeText(context, "Ilova ish holatida emas", Toast.LENGTH_LONG).show()
                    finishAffinity()

                }

                if (response.body()!!.status.equals("stopped")) {

                    check = true
                    Toast.makeText(context, "Obuna muddati tugagan ", Toast.LENGTH_LONG).show()
                    startActivity(Intent(context, LoginActivity::class.java))

                    finish()
                    clearUsername()

                }

            }

            override fun onFailure(call: Call<login_respons?>?, t: Throwable) {

                check = false

            }
        })

        return check

    }


    fun sendSms(number: String, message: String, i: Int) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            // Use Telephony API for sending SMS in Android 10 and above
            val smsManager = SmsManager.getDefault()
            smsManager.sendMultipartTextMessage(
                number,
                null,
                smsManager.divideMessage(message),
                null,
                null
            )
//            1 та 997621000 рақамига смс юборилди
            Toast.makeText(this, "$i - sms yuborildi", Toast.LENGTH_LONG).show()

        } else {

            val smsManager = SmsManager.getDefault()

            smsManager.sendMultipartTextMessage(
                number,
                null,
                smsManager.divideMessage(message),
                null,
                null
            )
            Toast.makeText(this, "$i - sms yuborildi", Toast.LENGTH_LONG).show()

        }

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

    @SuppressLint("SetTextI18n")
    fun setProgressDialog() {

        binding.sendFile.visibility = View.INVISIBLE

        // Creating a Linear Layout
        val llPadding = 30
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        // Creating a ProgressBar inside the layout
        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam
        llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER

        // Creating a TextView inside the layout
        val tvText = TextView(this)
        tvText.text = "Fayl yuklanmoqda ..."
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 17f
        tvText.layoutParams = llParam
        ll.addView(progressBar)
        ll.addView(tvText)

        // Setting the AlertDialog Builder view
        // as the Linear layout created above

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setView(ll)

        // Displaying the dialog
        dialog = builder.create()
        dialog.show()

        val window: Window? = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams

            // Disabling screen touch to avoid exiting the Dialog
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }
    }

    @SuppressLint("SetTextI18n")
    fun setProgressDialogSMS(text: String) {

        val llPadding = 30
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam
        llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER

        val tvText = TextView(this)
        tvText.text = text
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 17f
        tvText.layoutParams = llParam
        ll.addView(progressBar)
        ll.addView(tvText)

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setView(ll)

        dialogSMS = builder.create()
        dialogSMS.show()

        val window: Window? = dialogSMS.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialogSMS.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialogSMS.window?.attributes = layoutParams

            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun checkForInternet(context: Context): Boolean {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val network = connectivityManager.activeNetwork ?: return false

            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {

                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }


    fun checker() {


        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .create()
        val view = layoutInflater.inflate(R.layout.customview_layout, null)
        builder.setView(view)

        val button = view.findViewById<Button>(R.id.dialogDismiss_button)

        button.setOnClickListener {

            this.finishAffinity()

        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()

        AlertDialog.Builder(this, R.style.CustomAlertDialog)


    }


    private fun postDataUsingRetrofit(

        context: Context,
        number: String, password: String, file: String
    ) {
        var url = "https://smsboomber.pythonanywhere.com/api/v1/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val dataModel = respons_data(number, password, file, getCurrentHour().toString())
        val call: Call<data_model?>? = retrofitAPI.parseExs(dataModel)
        setProgressDialogSMS("SMSlar yuborilmoqda ...")

        call!!.enqueue(object : Callback<data_model?> {
            override fun onResponse(call: Call<data_model?>?, response: Response<data_model?>) {


//                startActivity(Intent(this@LoginActivity, MainActivity::class.java))

                try {
                    sendSms2(response.body()!!.data)

                } catch (t: Exception) {

                    Toast.makeText(
                        this@MainActivity,
                        "Iltimos to'g'ri file yuboring",
                        Toast.LENGTH_LONG
                    ).show()
                    dialogSMS.cancel()
                    binding.sendFile.visibility = View.VISIBLE

                }


            }

            override fun onFailure(call: Call<data_model?>?, t: Throwable) {

                Toast.makeText(context, "${t.message}", Toast.LENGTH_LONG).show()


            }
        })

    }

    private fun logout1(

        context: Context,
        number: String
    ) {
        var url = "https://smsboomber.pythonanywhere.com/api/v1/"
        setProgressDialogSMS("Iltimos kuting ...")
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val dataModel = logout_request(number,saveduid)
        val call: Call<login_respons?>? = retrofitAPI.loguot(dataModel)

        call!!.enqueue(object : Callback<login_respons?> {
            override fun onResponse(
                call: Call<login_respons?>?,
                response: Response<login_respons?>
            ) {


//                startActivity(Intent(this@LoginActivity, MainActivity::class.java))

                if (response.body()!!.status == "logout") {

                    logout()
                    dialogSMS.cancel()


                }


            }

            override fun onFailure(call: Call<login_respons?>?, t: Throwable) {

                Toast.makeText(context, "${t.message}", Toast.LENGTH_LONG).show()


            }
        })

    }


    fun sendSms2(data: ArrayList<phonedata_model>) {


        CoroutineScope(Dispatchers.Main).launch {

            var i = 1


            val list = data
            for (item in list) {

                item.phones.forEach { ii ->

                    sendSms(ii, item.message!!, i)



                    i++

                }

                delay(30000) // Delay for 1 second (1000 milliseconds)

            }



            i = 0

            dialogSMS.cancel()
            binding.sendFile.visibility = View.VISIBLE

        }

    }
}
