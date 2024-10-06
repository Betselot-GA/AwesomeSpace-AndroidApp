package com.example.exoplanetexploration.api


import com.example.exoplanetexploration.ExoplanetResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ExoplanetApiService {
    @POST("find-exoplanet")
    fun getExoplanets(@Body locationData: Map<String, Any>): Call<ExoplanetResponse>
}

