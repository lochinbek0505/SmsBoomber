package com.example.smsboomber.uitilts
import com.example.smsboomber.model.DataModel
import com.example.smsboomber.model.data_model
import com.example.smsboomber.model.login_respons
import com.example.smsboomber.model.logout_request
import com.example.smsboomber.model.respons_data
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitAPI {

    @POST("login/")
    fun login(@Body dataModel: DataModel?): Call<login_respons?>?


    @POST("parse/")
    fun parseExs(@Body dataModel: respons_data?): Call<data_model?>?

    @POST("logout/")
    fun loguot(@Body dataModel: logout_request?): Call<login_respons?>?


}
