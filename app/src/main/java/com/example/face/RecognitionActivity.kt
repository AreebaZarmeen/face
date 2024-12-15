package com.example.face

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import database.FaceDatabase
import utils.FaceAnalyzer
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecognitionActivity : AppCompatActivity() {
    private var db: FaceDatabase? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var recognitionText: TextView
    private lateinit var overlayView: OverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_recognition)

            // Initialize views
            initializeViews()

            // Initialize camera executor
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Check and request permissions
            if (allPermissionsGranted()) {
                initializeDatabase()
                startCamera()
            } else {
                requestPermissions()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize: ${e.localizedMessage}")
        }
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        recognitionText = findViewById(R.id.tvRecognitionResult)
        overlayView = OverlayView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        addContentView(overlayView, previewView.layoutParams)
    }

    private fun initializeDatabase() {
        try {
            db = FaceDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Database initialization failed", e)
            showError("Database initialization failed")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        try {
            // Ensure preview is initialized
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Post layout task to configure overlay size
            previewView.post {
                overlayView.setPreviewSize(Size(previewView.width, previewView.height))
            }

            Log.d(TAG, "Starting camera initialization")

            cameraProviderFuture.addListener({
                try {
                    Log.d(TAG, "Camera provider future listener triggered")
                    val cameraProvider = cameraProviderFuture.get()

                    // ImageAnalysis use case
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            Log.d(TAG, "ImageAnalysis use case built")
                            db?.let { database ->
                                analysis.setAnalyzer(cameraExecutor, FaceAnalyzer(database) { name, rect ->
                                    runOnUiThread {
                                        recognitionText.text = name
                                        overlayView.setFaceRect(rect)
                                    }
                                })
                            } ?: run {
                                Log.e(TAG, "Database is null when setting up analyzer")
                                showError("Database initialization error")
                            }
                        }

                    // CameraSelector for the front camera
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()

                    // Unbind and bind new use cases
                    try {
                        Log.d(TAG, "Unbinding all camera use cases")
                        cameraProvider.unbindAll()

                        Log.d(TAG, "Binding use cases to camera")
                        cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )

                        Log.d(TAG, "Camera setup completed successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Use case binding failed", e)
                        showError("Camera binding failed: ${e.localizedMessage}")
                    }

                } catch (e: ExecutionException) {
                    Log.e(TAG, "Camera provider future execution failed", e)
                    showError("Camera initialization failed: ${e.localizedMessage}")
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Camera provider future interrupted", e)
                    showError("Camera initialization interrupted")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during camera setup", e)
                    showError("Unexpected camera error: ${e.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(this))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera provider", e)
            showError("Failed to initialize camera: ${e.localizedMessage}")
        }
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        try {
            // Configure Preview
            val preview = Preview.Builder().build()

            // Configure ImageAnalysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    db?.let { database ->
                        analysis.setAnalyzer(cameraExecutor, FaceAnalyzer(database) { name, rect ->
                            runOnUiThread {
                                recognitionText.text = name
                                overlayView.setFaceRect(rect)
                            }
                        })
                    }
                }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Unbind previous use cases
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Attach surface provider
            preview.setSurfaceProvider(previewView.surfaceProvider)

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            showError("Camera setup failed")
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraExecutor.shutdown()
            db?.closeDatabase()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeDatabase()
                startCamera()
            } else {
                showError("Camera permission is required")
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "RecognitionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
/*
package com.example.face

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import database.FaceDatabase
import utils.FaceAnalyzer
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecognitionActivity : AppCompatActivity() {

    private var db: FaceDatabase? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var recognitionText: TextView
    private lateinit var detectionStateText: TextView
    private lateinit var overlayView: OverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_recognition)

            // Initialize views
            initializeViews()

            // Initialize camera executor
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Check and request permissions
            if (allPermissionsGranted()) {
                initializeDatabase()
                startCamera()
            } else {
                requestPermissions()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize: ${e.localizedMessage}")
        }
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        recognitionText = findViewById(R.id.tvRecognitionResult)
        detectionStateText = findViewById(R.id.tvDetectionState) // New TextView for detection state
        overlayView = OverlayView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        addContentView(overlayView, previewView.layoutParams)
    }

    private fun initializeDatabase() {
        try {
            db = FaceDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Database initialization failed", e)
            showError("Database initialization failed")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        try {
            // Ensure preview is initialized
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Post layout task to configure overlay size
            previewView.post {
                overlayView.setPreviewSize(Size(previewView.width, previewView.height))
            }

            Log.d(TAG, "Starting camera initialization")

            cameraProviderFuture.addListener({
                try {
                    Log.d(TAG, "Camera provider future listener triggered")
                    val cameraProvider = cameraProviderFuture.get()

                    // ImageAnalysis use case
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            Log.d(TAG, "ImageAnalysis use case built")
                            db?.let { database ->
                                analysis.setAnalyzer(cameraExecutor, FaceAnalyzer(
                                    database,
                                    onFaceRecognized = { name, rect ->
                                        runOnUiThread {
                                            recognitionText.text = name
                                            overlayView.setFaceRect(rect)
                                        }
                                    },
                                    onDetectionStateChanged = { state ->
                                        runOnUiThread {
                                            updateDetectionState(state)
                                        }
                                    }
                                ))
                            } ?: run {
                                Log.e(TAG, "Database is null when setting up analyzer")
                                showError("Database initialization error")
                            }
                        }

                    // CameraSelector for the front camera
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()

                    // Unbind and bind new use cases
                    try {
                        Log.d(TAG, "Unbinding all camera use cases")
                        cameraProvider.unbindAll()

                        Log.d(TAG, "Binding use cases to camera")
                        cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )

                        Log.d(TAG, "Camera setup completed successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Use case binding failed", e)
                        showError("Camera binding failed: ${e.localizedMessage}")
                    }

                } catch (e: ExecutionException) {
                    Log.e(TAG, "Camera provider future execution failed", e)
                    showError("Camera initialization failed: ${e.localizedMessage}")
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Camera provider future interrupted", e)
                    showError("Camera initialization interrupted")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during camera setup", e)
                    showError("Unexpected camera error: ${e.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(this))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera provider", e)
            showError("Failed to initialize camera: ${e.localizedMessage}")
        }
    }

    private fun updateDetectionState(state: FaceAnalyzer.DetectionState) {
        when (state) {
            FaceAnalyzer.DetectionState.IDLE -> {
                detectionStateText.text = "Idle"
                detectionStateText.setTextColor(Color.GRAY)
            }
            FaceAnalyzer.DetectionState.DETECTING -> {
                detectionStateText.text = "Detecting..."
                detectionStateText.setTextColor(Color.YELLOW)
            }
            FaceAnalyzer.DetectionState.RECOGNIZED -> {
                detectionStateText.text = "Face Recognized"
                detectionStateText.setTextColor(Color.GREEN)
            }
            FaceAnalyzer.DetectionState.FAILED -> {
                detectionStateText.text = "Detection Failed"
                detectionStateText.setTextColor(Color.RED)
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraExecutor.shutdown()
            db?.closeDatabase()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeDatabase()
                startCamera()
            } else {
                showError("Camera permission is required")
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "RecognitionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
*/