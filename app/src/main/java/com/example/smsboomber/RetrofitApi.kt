package com.example.smsboomber
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface RetrofitAPI {

    @POST("login/")
    fun login(@Body dataModel: DataModel?): Call<login_respons?>?


    @POST("parse/")
    fun parseExs(@Body dataModel: respons_data?): Call<data_model?>?

    @POST("logout/")
    fun loguot(@Body dataModel: logout_request?): Call<login_respons?>?


}
