package com.example.exoplanetexploration

data class ExoplanetResponse(
    val message: String,
    val data: Data
)

data class Data(
    val exoplanet: Exoplanet
)