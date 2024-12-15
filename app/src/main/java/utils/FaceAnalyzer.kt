package utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import database.FaceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeoutException

class FaceAnalyzer(
    private val db: FaceDatabase,
    private val onFaceRecognized: (String, Rect?) -> Unit
) : ImageAnalysis.Analyzer {

    // Configure face detector for quick and basic detection
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                // Detect faces
                faceDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            val firstFace = faces[0]
                            val boundingBox = firstFace.boundingBox

                            Log.d("FaceAnalyzer", "Face detected: $boundingBox")

                            CoroutineScope(Dispatchers.Default).launch {
                                val bitmap = convertImageProxyToBitmap(imageProxy)
                                if (bitmap == null) {
                                    Log.e("FaceAnalyzer", "Failed to convert ImageProxy to Bitmap")
                                    withContext(Dispatchers.Main) {
                                        onFaceRecognized("Bitmap conversion failed", boundingBox)
                                    }
                                    imageProxy.close()
                                    return@launch
                                }

                                try {
                                    Log.d("FaceAnalyzer", "Bitmap converted: width = ${bitmap.width}, height = ${bitmap.height}")

                                    // Perform face recognition
                                    val recognizedName = recognizeFace(bitmap)

                                    withContext(Dispatchers.Main) {
                                        Log.d("FaceAnalyzer", "Recognition result: $recognizedName")
                                        onFaceRecognized(recognizedName, boundingBox)
                                    }
                                } catch (e: Exception) {
                                    handleRecognitionError(e, boundingBox)
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        } else {
                            Log.d("FaceAnalyzer", "No face detected")
                            onFaceRecognized("No face", null)
                            imageProxy.close()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FaceAnalyzer", "Face detection failed: ${e.message}")
                        onFaceRecognized("Detection failed", null)
                        imageProxy.close()
                    }
            } else {
                Log.e("FaceAnalyzer", "MediaImage is null")
                imageProxy.close()
            }
        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Unhandled error: ${e.message}")
            imageProxy.close()
        }
    }

    // Simple face recognition method
    // In FaceAnalyzer.kt

    private fun recognizeFace(bitmap: Bitmap): String {
        try {
            val storedFaces = db.getAllFaces()
            Log.d("FaceAnalyzer", "Stored faces: ${storedFaces.size}")
            if (storedFaces.isEmpty()) return "No stored faces"

            // Generate embedding for current face
            val currentEmbedding = EmbeddingUtils.generateEmbedding(bitmap)
            var bestMatch: String? = null
            var bestSimilarity = 0.0

            for (storedFace in storedFaces) {
                try {
                    // Load stored face bitmap
                    val storedBitmap = BitmapFactory.decodeFile(storedFace.imagePath)
                    if (storedBitmap != null) {
                        // Generate embedding for stored face
                        val storedEmbedding = EmbeddingUtils.generateEmbedding(storedBitmap)
                        // Compare embeddings
                        val similarity = EmbeddingUtils.compareFaces(currentEmbedding, storedEmbedding)

                        if (similarity > bestSimilarity && similarity > 0.6) { // Threshold of 0.6
                            bestSimilarity = similarity
                            bestMatch = storedFace.name
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FaceAnalyzer", "Error comparing faces: ${e.message}")
                }
            }
            return bestMatch ?: "Unknown"
        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Recognition error: ${e.message}")
            throw e
        }
    }
    // Basic similarity calculation
    private fun calculateBasicSimilarity(bitmap1: Bitmap, imagePath: String): Float {
        return try {
            val storedBitmap = BitmapFactory.decodeFile(imagePath) ?: return 0f

            // Resize for comparison
            val resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1, 100, 100, false)
            val resizedStoredBitmap = Bitmap.createScaledBitmap(storedBitmap, 100, 100, false)

            var similarityScore = 0f
            var totalPixels = 0

            for (x in 0 until resizedBitmap1.width) {
                for (y in 0 until resizedBitmap1.height) {
                    val pixel1 = resizedBitmap1.getPixel(x, y)
                    val pixel2 = resizedStoredBitmap.getPixel(x, y)

                    val redDiff = Math.abs(android.graphics.Color.red(pixel1) - android.graphics.Color.red(pixel2))
                    val greenDiff = Math.abs(android.graphics.Color.green(pixel1) - android.graphics.Color.green(pixel2))
                    val blueDiff = Math.abs(android.graphics.Color.blue(pixel1) - android.graphics.Color.blue(pixel2))

                    similarityScore += (255 - (redDiff + greenDiff + blueDiff) / 3).toFloat()
                    totalPixels++
                }
            }
            similarityScore / totalPixels
        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Error calculating similarity: ${e.message}")
            0f
        }
    }

    // Convert ImageProxy to Bitmap
    private fun convertImageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            Log.e("FaceAnalyzer", "Unsupported image format: ${imageProxy.format}")
            return null
        }

        return try {
            val planes = imageProxy.planes
            val bufferY = planes[0].buffer
            val bufferU = planes[1].buffer
            val bufferV = planes[2].buffer

            val ySize = bufferY.remaining()
            val uSize = bufferU.remaining()
            val vSize = bufferV.remaining()

            val yuvData = ByteArray(ySize + uSize + vSize)
            bufferY.get(yuvData, 0, ySize)
            bufferU.get(yuvData, ySize, uSize)
            bufferV.get(yuvData, ySize + uSize, vSize)

            val yuvImage = YuvImage(yuvData, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val outStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, outStream)
            BitmapFactory.decodeByteArray(outStream.toByteArray(), 0, outStream.size())
        } catch (e: IllegalArgumentException) {
            Log.e("FaceAnalyzer", "Error converting ImageProxy to Bitmap: ${e.message}")
            null
        }
    }

    // Handle recognition errors
    private suspend fun handleRecognitionError(e: Exception, boundingBox: Rect?) {
        Log.e("FaceAnalyzer", "Recognition error: ${e.message}")
        val errorDetails = "Error: ${e::class.java.simpleName} - ${e.message}"
        withContext(Dispatchers.Main) {
            onFaceRecognized(errorDetails, boundingBox)
        }
    }
}
/*
package utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetector
import database.FaceDatabase
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

class FaceAnalyzer(
    private val db: FaceDatabase,
    private val onFaceRecognized: (String, Rect?) -> Unit,
    private val onDetectionStateChanged: (DetectionState) -> Unit
) : ImageAnalysis.Analyzer {

    enum class DetectionState {
        IDLE,
        DETECTING,
        RECOGNIZED,
        FAILED
    }

    companion object {
        private const val DETECTION_COOLDOWN_MS = 1000L // Increased cooldown
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val FACE_SIZE_RATIO_MIN = 0.1f // Slightly increased minimum face size
        private const val FACE_SIZE_RATIO_MAX = 0.8f
        private const val MAX_CONSECUTIVE_FAILURES = 3 // New limit for consecutive failures
    }

    private var lastDetectionTime = 0L
    private var isProcessing = false
    private var detectedBitmap: Bitmap? = null
    private var consecutiveFailures = 0 // Track consecutive detection failures

    private val faceDetector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Changed to fast mode
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // Removed unnecessary landmark detection
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(FACE_SIZE_RATIO_MIN)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // More robust processing check
        synchronized(this) {
            if (currentTime - lastDetectionTime < DETECTION_COOLDOWN_MS || isProcessing) {
                imageProxy.close()
                return
            }
            isProcessing = true
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            handleError("MediaImage is null", null, imageProxy)
            return
        }

        onDetectionStateChanged(DetectionState.DETECTING)

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val firstFace = faces[0]
                    val boundingBox = firstFace.boundingBox

                    val imageWidth = imageProxy.width
                    val imageHeight = imageProxy.height

                    val faceWidthRatio = boundingBox.width().toFloat() / imageWidth
                    val faceHeightRatio = boundingBox.height().toFloat() / imageHeight

                    val expandedBoundingBox = Rect(
                        max(0, boundingBox.left - boundingBox.width() / 4),
                        max(0, boundingBox.top - boundingBox.height() / 4),
                        min(imageWidth, boundingBox.right + boundingBox.width() / 4),
                        min(imageHeight, boundingBox.bottom + boundingBox.height() / 4)
                    )

                    val isValidFaceSize = (faceWidthRatio in FACE_SIZE_RATIO_MIN..FACE_SIZE_RATIO_MAX) &&
                            (faceHeightRatio in FACE_SIZE_RATIO_MIN..FACE_SIZE_RATIO_MAX)

                    if (isValidFaceSize) {
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val bitmap = convertImageProxyToBitmap(imageProxy)
                                detectedBitmap = bitmap
                                if (bitmap == null) {
                                    handleError("Bitmap conversion failed", expandedBoundingBox, imageProxy)
                                    return@launch
                                }

                                val faceCrop = Bitmap.createBitmap(
                                    bitmap,
                                    expandedBoundingBox.left,
                                    expandedBoundingBox.top,
                                    expandedBoundingBox.width(),
                                    expandedBoundingBox.height()
                                )

                                val matchedName = db.findMatchingFace(faceCrop)
                                if (matchedName != null) {
                                    onFaceRecognized(matchedName, expandedBoundingBox)
                                    onDetectionStateChanged(DetectionState.RECOGNIZED)
                                    consecutiveFailures = 0 // Reset failure counter on success
                                } else {
                                    consecutiveFailures++
                                    if (consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                                        Log.d("FaceAnalyzer", "No matching face found. Attempt ${consecutiveFailures}")
                                        onDetectionStateChanged(DetectionState.FAILED)
                                    } else {
                                        // Reset after reaching maximum consecutive failures
                                        consecutiveFailures = 0
                                        onDetectionStateChanged(DetectionState.IDLE)
                                    }
                                }
                            } catch (e: Exception) {
                                handleError("Face processing error: ${e.message}", expandedBoundingBox, imageProxy)
                            } finally {
                                synchronized(this@FaceAnalyzer) {
                                    isProcessing = false
                                    lastDetectionTime = System.currentTimeMillis()
                                }
                                detectedBitmap?.recycle()
                                imageProxy.close()
                            }
                        }
                    } else {
                        handleError("Invalid face size", null, imageProxy)
                    }
                } else {
                    consecutiveFailures++
                    if (consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                        Log.d("FaceAnalyzer", "No faces detected. Attempt ${consecutiveFailures}")
                        onDetectionStateChanged(DetectionState.FAILED)
                    } else {
                        // Reset after reaching maximum consecutive failures
                        consecutiveFailures = 0
                        onDetectionStateChanged(DetectionState.IDLE)
                    }
                    imageProxy.close()
                }
            }
            .addOnFailureListener { exception ->
                handleError("Face detection failed: ${exception.message}", null, imageProxy)
            }
    }

    // Rest of the implementation remains the same as in the original code...
    private fun recognizeFace(bitmap: Bitmap): String {
        try {
            val storedFaces = db.getAllFaces()
            Log.d("FaceAnalyzer", "Stored faces: ${storedFaces.size}")
            if (storedFaces.isEmpty()) return "No stored faces"

            // Generate embedding for current face
            val currentEmbedding = EmbeddingUtils.generateEmbedding(bitmap)
            var bestMatch: String? = null
            var bestSimilarity = 0.0

            for (storedFace in storedFaces) {
                try {
                    // Load stored face bitmap
                    val storedBitmap = BitmapFactory.decodeFile(storedFace.imagePath)
                    if (storedBitmap != null) {
                        // Generate embedding for stored face
                        val storedEmbedding = EmbeddingUtils.generateEmbedding(storedBitmap)
                        // Compare embeddings
                        val similarity = EmbeddingUtils.compareFaces(currentEmbedding, storedEmbedding)

                        if (similarity > bestSimilarity && similarity > 0.6) { // Threshold of 0.6
                            bestSimilarity = similarity
                            bestMatch = storedFace.name
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FaceAnalyzer", "Error comparing faces: ${e.message}")
                }
            }
            return bestMatch ?: "Unknown"
        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Recognition error: ${e.message}")
            throw e
        }
    }
    // Basic similarity calculation
    private fun calculateBasicSimilarity(bitmap1: Bitmap, imagePath: String): Float {
        return try {
            val storedBitmap = BitmapFactory.decodeFile(imagePath) ?: return 0f

            // Resize for comparison
            val resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1, 100, 100, false)
            val resizedStoredBitmap = Bitmap.createScaledBitmap(storedBitmap, 100, 100, false)

            var similarityScore = 0f
            var totalPixels = 0

            for (x in 0 until resizedBitmap1.width) {
                for (y in 0 until resizedBitmap1.height) {
                    val pixel1 = resizedBitmap1.getPixel(x, y)
                    val pixel2 = resizedStoredBitmap.getPixel(x, y)

                    val redDiff = Math.abs(android.graphics.Color.red(pixel1) - android.graphics.Color.red(pixel2))
                    val greenDiff = Math.abs(android.graphics.Color.green(pixel1) - android.graphics.Color.green(pixel2))
                    val blueDiff = Math.abs(android.graphics.Color.blue(pixel1) - android.graphics.Color.blue(pixel2))

                    similarityScore += (255 - (redDiff + greenDiff + blueDiff) / 3).toFloat()
                    totalPixels++
                }
            }
            similarityScore / totalPixels
        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Error calculating similarity: ${e.message}")
            0f
        }
    }

    // Convert ImageProxy to Bitmap
    private fun convertImageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            Log.e("FaceAnalyzer", "Unsupported image format: ${imageProxy.format}")
            return null
        }

        return try {
            val planes = imageProxy.planes
            val bufferY = planes[0].buffer
            val bufferU = planes[1].buffer
            val bufferV = planes[2].buffer

            val ySize = bufferY.remaining()
            val uSize = bufferU.remaining()
            val vSize = bufferV.remaining()

            val yuvData = ByteArray(ySize + uSize + vSize)
            bufferY.get(yuvData, 0, ySize)
            bufferU.get(yuvData, ySize, uSize)
            bufferV.get(yuvData, ySize + uSize, vSize)

            val yuvImage = YuvImage(yuvData, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val outStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, outStream)
            BitmapFactory.decodeByteArray(outStream.toByteArray(), 0, outStream.size())
        } catch (e: IllegalArgumentException) {
            Log.e("FaceAnalyzer", "Error converting ImageProxy to Bitmap: ${e.message}")
            null
        }
    }
    private fun handleError(message: String, boundingBox: Rect?, imageProxy: ImageProxy) {
        Log.d("FaceAnalyzer", message) // Changed to Log.d for less intrusive logging

        synchronized(this) {
            isProcessing = false
            lastDetectionTime = System.currentTimeMillis()
        }

        // Prevent showing repeated failure messages
        if (consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
            onDetectionStateChanged(DetectionState.FAILED)
        } else {
            onDetectionStateChanged(DetectionState.IDLE)
        }

        imageProxy.close()
    }

    fun cleanup() {
        faceDetector.close()
    }
}*/