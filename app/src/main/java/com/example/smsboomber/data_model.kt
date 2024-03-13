package com.example.smsboomber

import com.google.gson.annotations.SerializedName

data class data_model(


    @SerializedName("data" ) var data : ArrayList<phonedata_model> = arrayListOf(),
    @SerializedName("days") var days:String

)
