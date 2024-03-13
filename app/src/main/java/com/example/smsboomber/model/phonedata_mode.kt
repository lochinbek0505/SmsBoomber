package com.example.smsboomber.model

import com.google.gson.annotations.SerializedName

data class phonedata_model(

    @SerializedName("phones") var phones: ArrayList<String> = arrayListOf(),
    @SerializedName("message") var message: String


)
