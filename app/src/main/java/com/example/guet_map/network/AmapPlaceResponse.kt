package com.example.guet_map.network

import com.google.gson.annotations.SerializedName

data class AmapPlaceResponse(
    val status: String,
    val info: String? = null,
    val pois: List<AmapPlacePoi>? = null
)

data class AmapPlacePoi(
    val id: String? = null,
    val name: String? = null,
    val location: String? = null,
    val type: String? = null,
    val address: String? = null,
    @SerializedName("pname")
    val province: String? = null,
    @SerializedName("cityname")
    val city: String? = null,
    @SerializedName("adname")
    val district: String? = null
)
