package com.example.exoplanetexploration
import kotlin.math.*
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.exoplanetexploration.api.ExoplanetApiService
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.ar.core.ArCoreApk
import com.google.ar.core.HitResult
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.ux.TransformableNode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var exoplanetApiService: ExoplanetApiService
    private var foundExoplanet: Exoplanet? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var scanButton: Button
    // Define constants
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    // UI Elements
    private lateinit var exoplanetTitle: TextView
    private lateinit var exoplanetInfo: TextView
    private lateinit var exoplanetImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AR Fragment
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
// Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Check if Google Play Services for AR is available
        if (!isGooglePlayServicesAvailable()) {
            Toast.makeText(this, "Google Play Services for AR not available", Toast.LENGTH_SHORT).show()
            return // Early exit to avoid initializing further AR components
        }

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            val CAMERA_REQUEST_CODE = 1
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }


        try {
            arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize AR: ${e.message}", Toast.LENGTH_LONG).show()
        }


        scanButton = findViewById(R.id.scan_button)


        // Initialize UI elements
        exoplanetTitle = findViewById(R.id.exoplanet_title)
        exoplanetInfo = findViewById(R.id.exoplanet_info)
        exoplanetImage = findViewById(R.id.exoplanet_image)

        // Initialize FusedLocationProviderClient for GPS location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
// Set the initial background color of the button
       // scanButton.setBackgroundColor(Color.parseColor("#FF0000")) // Change this to your desired color


        // Set up scan button listener
        scanButton.setOnClickListener {
            handleScanButtonClick()
        }

        // Retrofit setup
        val retrofit = Retrofit.Builder()
            .baseUrl("https://awesomespace.onrender.com/api/fin-exoplanet")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        exoplanetApiService = retrofit.create(ExoplanetApiService::class.java)

        // Trigger the scan for exoplanets

    }

    private fun requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show an explanation to the user why location permission is needed
            Toast.makeText(this, "Location permission is required to scan for exoplanets.", Toast.LENGTH_LONG).show()
        }
        // Request the location permission
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }
    private fun isGooglePlayServicesAvailable(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        return availability.isSupported && availability != ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
    }

    private fun handleScanButtonClick() {
        // Check if location permissions are granted before proceeding
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission if not granted
            requestLocationPermissions()
        } else {
            // If permission is already granted, proceed to get current location and fetch exoplanets
            getCurrentLocation()
        }
    }


    fun calculateAzimuth(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Convert degrees to radians
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val lambda1 = Math.toRadians(lon1)
        val lambda2 = Math.toRadians(lon2)

        val deltaLambda = lambda2 - lambda1

        val x = cos(phi2) * sin(deltaLambda)
        val y = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        // Calculate azimuth in radians
        val azimuthRadians = atan2(x, y)

        // Convert radians to degrees
        var azimuthDegrees = Math.toDegrees(azimuthRadians)

        // Normalize azimuth to be within 0-360 degrees
        if (azimuthDegrees < 0) {
            azimuthDegrees += 360
        }

        return azimuthDegrees
    }


    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // Use the location data
                        val latitude = location.latitude
                        val longitude = location.longitude
                        var location_text = ""
                        val azimuth = calculateAzimuth(latitude, longitude, 0.0, 0.0)
                        location_text = "Latitude: $latitude, Longitude: $longitude"
                        Toast.makeText(this, location_text, Toast.LENGTH_LONG).show()
                        fetchExoplanets(latitude,longitude,azimuth,location.altitude)


                        // Proceed with your scanning logic or fetching exoplanets based on location
                    } else {
                        Toast.makeText(this, "Unable to retrieve location. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get location.", Toast.LENGTH_LONG).show()
                }
        } else {
            // Permission is not granted, request permission
            requestLocationPermissions()
        }
    }

    private fun scanForExoplanets() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        // Get the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val azimuth = 10.0 // Placeholder for actual azimuth value
                val altitude = 230.0 // Placeholder for actual altitude value

                // Send the current location data to fetch exoplanets
                fetchExoplanets(latitude, longitude, azimuth, altitude)
            } else {
                Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchExoplanets(latitude: Double, longitude: Double, azimuth: Double, altitude: Double) {
        val locationData = HashMap<String, Double>()
        locationData["latitude"] = latitude
        locationData["longitude"] = longitude
        locationData["azimuth"] = azimuth
        locationData["altitude"] = altitude

        exoplanetApiService.getExoplanets(locationData).enqueue(object : Callback<ExoplanetResponse> {
            override fun onResponse(call: Call<ExoplanetResponse>, response: Response<ExoplanetResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { exoplanetResponse ->
                        val exoplanet = exoplanetResponse.data.exoplanet
                        Log.d("Exoplanet Details", exoplanet.getInfo())

                        // Show the exoplanet name, info, and load the image
                        exoplanetTitle.text = exoplanet.name
                        exoplanetInfo.text = exoplanet.getInfo()

                        // Load the image using Glide
                        Glide.with(this@MainActivity)
                            .load("res/drawable/exoplanet.png") // Update this with your actual image URL field
                            .placeholder(R.drawable.exoplanet) // Placeholder image
                            .into(exoplanetImage)

                        // Make UI elements visible
                        exoplanetTitle.visibility = View.VISIBLE
                        exoplanetInfo.visibility = View.VISIBLE
                        exoplanetImage.visibility = View.VISIBLE

                        // Store the found exoplanet and load the 3D model
                        foundExoplanet = exoplanet
                        loadModel()
                    } ?: run {
                        Toast.makeText(this@MainActivity, "Response body is null.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to fetch exoplanets. Error code: ${response.code()}", Toast.LENGTH_LONG).show()
                    Log.e("API_ERROR", "Response: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ExoplanetResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to fetch exoplanets: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("API_FAILURE", "Error: ${t.message}")
            }
        })
    }
    private fun loadModel() {
        foundExoplanet?.let { exoplanet ->
            ModelRenderable.builder()
                .setSource(this, Uri.parse("assets/model1.glb")) // Change to your actual model file path
                .build()
                .thenAccept { modelRenderable: ModelRenderable ->
                    addModelToScene(modelRenderable)
                }
                .exceptionally { throwable: Throwable ->
                    Toast.makeText(this, "Unable to load model: ${throwable.message}", Toast.LENGTH_LONG).show()
                    null
                }
        }
    }
    private fun addModelToScene(modelRenderable: ModelRenderable) {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane, motionEvent ->
            val anchor: Anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            val modelNode = TransformableNode(arFragment.transformationSystem)
            modelNode.setParent(anchorNode)
            modelNode.renderable = modelRenderable
            modelNode.select()
        }
    }

    private fun placeModelInAR(renderable: ModelRenderable) {
        val session: Session? = arFragment.arSceneView.session
        val cameraPose = arFragment.arSceneView.arFrame?.camera?.displayOrientedPose // Fix the unresolved 'camera' reference

        if (session != null && cameraPose != null) {
            // Create an anchor at a specific distance in front of the camera
            val anchor = session.createAnchor(
                cameraPose.compose(
                    com.google.ar.core.Pose.makeTranslation(0f, 0f, -1f) // Adjust translation distance as needed
                )
            )

            // Attach the renderable model to the anchor
            val anchorNode = AnchorNode(anchor)
            anchorNode.renderable = renderable
            arFragment.arSceneView.scene.addChild(anchorNode)
        } else {
            Toast.makeText(this, "AR Session or Camera Pose not initialized", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showExoplanetImage() {
        val exoplanet = foundExoplanet ?: return

        // Assuming the exoplanet object has an image URL or fetch it from your API
       // val exoplanetImageUrl = "https://example.com/images/${exoplanet.name}.jpg" // Replace with actual image URL logic
        val exoplanetImageUrl = "res/drawable/exoplanet.png"
        val imageView: ImageView = findViewById(R.id.exoplanet_image)

        // Use Glide or any image loading library to load the image
        Glide.with(this)
            .load(exoplanetImageUrl)
            .placeholder(R.drawable.exoplanet) // Placeholder image
            //.error(R.drawable.error_image) // Error image
            .into(imageView)
    }

    // Handle location permission result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now proceed to get location
                getCurrentLocation()
            } else {
                // Permission denied, notify the user
                Toast.makeText(this, "Location permission denied. Cannot scan for exoplanets.", Toast.LENGTH_LONG).show()
            }
        }
    }

}
