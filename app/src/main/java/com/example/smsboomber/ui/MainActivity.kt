package com.example.smsboomber.ui

import com.example.smsboomber.uitilts.DatabaseHandler
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.provider.OpenableColumns
import android.telephony.SmsManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smsboomber.R
import com.example.smsboomber.uitilts.RetrofitAPI
import com.example.smsboomber.databinding.ActivityMainBinding
import com.example.smsboomber.model.DataModel
import com.example.smsboomber.model.Message
import com.example.smsboomber.model.data_model
import com.example.smsboomber.model.login_respons
import com.example.smsboomber.model.logout_request
import com.example.smsboomber.model.phone_model
import com.example.smsboomber.model.phonedata_model
import com.example.smsboomber.model.respons_data
import com.example.smsboomber.uitilts.CatecoriesAdapter
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
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
    private lateinit var dialogSMS2: AlertDialog

    lateinit var dataList:MutableList<phone_model>
    private val SMS_PERMISSION_REQUEST_CODE = 101
    var savedUsername = ""
    var savedPassport = ""
    var saveduid = ""
    var uploadFile = false
    var coroutineJob: Job? = null
    lateinit var db: DatabaseHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        savedUsername = sharedPreferences.getString(KEY_NAME, "").toString()
        savedPassport = sharedPreferences.getString(KEY_NAME2, "").toString()
        saveduid = sharedPreferences.getString("uuid", "").toString()
        db = DatabaseHandler(this)
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

        binding.exit.setOnClickListener {

            finishAffinity()

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

        binding.tvHistory.setOnClickListener {

            historyDialog()

        }

        binding.sendFile.setOnClickListener {

            val galleryIntent = Intent(Intent.ACTION_PICK)
            binding.exit.visibility = View.INVISIBLE
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//
            galleryIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            startActivityForResult(galleryIntent, 1);
            db.deleteAllMessages()

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

//                    dialog.cancel()
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
        val dataModel = DataModel(saveduid, number, password, getCurrentHour().toString(), "main")
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
            Toast.makeText(this, "$i - SMS  $number raqamiga (SMS) yuborildi.", Toast.LENGTH_LONG).show()

        } else {

            val smsManager = SmsManager.getDefault()

            smsManager.sendMultipartTextMessage(
                number,
                null,
                smsManager.divideMessage(message),
                null,
                null
            )
            Toast.makeText(this, "$i - SMS  $number raqamiga yuborildi.", Toast.LENGTH_LONG).show()

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

    fun historyDialog() {


        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .create()
        val view = layoutInflater.inflate(R.layout.customview_layout3, null)
        builder.setView(view)

        val history = view.findViewById<RecyclerView>(R.id.recycler3)
        val back = view.findViewById<ImageView>(R.id.iv_back)

        back.setOnClickListener {

            builder.cancel()

        }


        val dd = db.getAllMessages()
        val adapter = CatecoriesAdapter(dd, object : CatecoriesAdapter.ItemSetOnClickListener {
            override fun onClick(data: Message) {

//                sendSms(data.number,data.message,data.messageIndex.toInt())
                sendSms(data.number, data.message, data.messageIndex.toInt())

            }


        })

        history.adapter = adapter


//        history.setOnClickListener {
//
//            startActivity(Intent(this, HIsotryActivity::class.java))
//
//
//        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()

        AlertDialog.Builder(this, R.style.CustomAlertDialog)


    }

    fun sendSmsDialog() {


        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .create()
        dialogSMS2 = builder
        val view = layoutInflater.inflate(R.layout.customview_layout2, null)
        builder.setView(view)

        val history = view.findViewById<Button>(R.id.btn_history_cl2)
        val pause = view.findViewById<ImageView>(R.id.iv_cl2)
        val progressBar=view.findViewById<ProgressBar>(R.id.cl2_pd)
        val message=view.findViewById<TextView>(R.id.tv_message_cl2)
        val back=view.findViewById<TextView>(R.id.tv_cancel)


        back.setOnClickListener {


            coroutineJob!!.cancel()

            binding.exit.visibility = View.VISIBLE
            binding.sendFile.visibility = View.VISIBLE
            dialogSMS2.cancel()
        }
        var check = true
        pause.setOnClickListener {

            if (check) {
                coroutineJob?.cancel()
                pause.setImageResource(R.drawable.ic_play)
                Log.e("HomeFragmenttt",dataList.toString())
                progressBar.visibility=View.INVISIBLE
                message.text="SMS yuborilishi to'xtatilgan"
                check = false
            } else {
                val index=dataList.get(0).index
                val list= mutableListOf<phone_model>()

                progressBar.visibility=View.VISIBLE
                message.text="SMS lar yuborilmoqda ..."
                    list.add(phone_model(dataList.get(0).list,index))


                sendSms2(list)
                pause.setImageResource(R.drawable.ic_pause)
                check = true
                Log.e("HomeFragmenttt",dataList.toString())

            }


        }

        history.setOnClickListener {

            historyDialog()

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
//        setProgressDialogSMS("SMSlar yuborilmoqda ...")

        dialog.cancel()

        call!!.enqueue(object : Callback<data_model?> {
            override fun onResponse(call: Call<data_model?>?, response: Response<data_model?>) {


//                startActivity(Intent(this@LoginActivity, MainActivity::class.java))

                try {
                    val data = response.body()!!.data
                    sendSmsDialog()
//                    yourClassInstance.sendSms2(data, this@MainActivity)
                    val mm= mutableListOf<phone_model>()
                    mm.add(phone_model(data,1))
                    Log.e("HomeAAA",data.toString())

                    sendSms2(mm)


                } catch (t: Exception) {

                    Toast.makeText(
                        this@MainActivity,
                        "Iltimos to'g'ri file yuboring",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("HomeAAA",t.message.toString())
                    dialogSMS2.cancel()
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
        val dataModel = logout_request(number, saveduid)
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


    fun sendSms2(data: MutableList<phone_model>){
        dataList = data

        coroutineJob = CoroutineScope(Dispatchers.Main).launch {

            try {
                for (item in dataList.get(0).list.toList()) {
                    item.phones.forEach { ii ->
                        // Check if the coroutine is still active before sending the SMS
                        if (isActive) {
                            sendSms(ii, item.message!!, dataList.get(0).index)
                            db.addMessage(ii, item.message, dataList.get(0).index.toString())
                            dataList.get(0).index++
                        } else {
                            // Handle the case where coroutine is cancelled
                            return@launch
                        }
                    }
                    dataList.get(0).list.remove(item)

                    delay(30000) // Delay for 3 seconds
                }
                Toast.makeText(
                    this@MainActivity,
                    "Hamma SMS lar yuborilib bo'lindi",
                    Toast.LENGTH_LONG
                ).show()
                binding.exit.visibility = View.VISIBLE
                binding.sendFile.visibility = View.VISIBLE

//                dialogSMS2.cancel()
            } catch (e: CancellationException) {


                println("Coroutine cancelled")
            }
        }

    }

}
