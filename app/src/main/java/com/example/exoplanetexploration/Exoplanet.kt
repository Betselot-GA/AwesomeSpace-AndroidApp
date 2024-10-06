package com.example.exoplanetexploration

import kotlinx.serialization.Serializable

@Serializable
data class Exoplanet(
    val name: String,
    val distance_from_earth: String,
    val orbital_period: String,
    val radius: String,
    val mass: String,
    val star_type: String,
    val habitability_factors: String,
    val ra: Double,
    val dec: Double,
    val latitude: Double?,
    val longitude: Double?
) {
    fun getInfo(): String {
        return """
            Name: $name
            Distance from Earth: $distance_from_earth
            Orbital Period: $orbital_period
            Radius: $radius
            Mass: $mass
            Star Type: $star_type
            Habitability Factors: $habitability_factors
            Right Ascension (RA): $ra
            Declination (Dec): $dec
        """.trimIndent()
    }
}
