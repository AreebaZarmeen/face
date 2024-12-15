//
//
//package utils
//
//import android.graphics.*
//import androidx.camera.core.ImageProxy
//import kotlin.math.*
//
//object EmbeddingUtils {
//    private const val EMBEDDING_DIM = 128
//    private const val GRID_SIZE = 16
//    private const val LBP_RADIUS = 1
//    private const val STANDARD_SIZE = 160
//
//    fun generateEmbedding(bitmap: Bitmap): ByteArray {
//        try {
//            // Step 1: Initial processing
//            val processedBitmap = preprocessImage(bitmap)
//
//            // Step 2: Extract features
//            val lbpFeatures = extractLBPFeatures(processedBitmap)
//            val intensityFeatures = extractIntensityFeatures(processedBitmap)
//            val edgeFeatures = extractEdgeFeatures(processedBitmap)
//
//            // Step 3: Combine features into final embedding
//            return combineFeatures(lbpFeatures, intensityFeatures, edgeFeatures)
//        } catch (e: Exception) {
//            e.printStackTrace()  // Log the error for debugging
//            // Fallback to a simpler method if any step fails
//            return generateSimpleEmbedding(bitmap)
//        }
//    }
//
//    private fun preprocessImage(bitmap: Bitmap): Bitmap {
//        return try {
//            // Convert to standard size
//            val scaled = Bitmap.createScaledBitmap(bitmap, STANDARD_SIZE, STANDARD_SIZE, true)
//
//            // Convert to grayscale
//            val grayscale = Bitmap.createBitmap(STANDARD_SIZE, STANDARD_SIZE, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(grayscale)
//            val paint = Paint().apply {
//                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
//            }
//            canvas.drawBitmap(scaled, 0f, 0f, paint)
//
//            // Normalize brightness
//            val pixels = IntArray(STANDARD_SIZE * STANDARD_SIZE)
//            grayscale.getPixels(pixels, 0, STANDARD_SIZE, 0, 0, STANDARD_SIZE, STANDARD_SIZE)
//
//            var min = 255
//            var max = 0
//            pixels.forEach { pixel ->
//                val value = Color.red(pixel)
//                min = minOf(min, value)
//                max = maxOf(max, value)
//            }
//
//            val range = max - min
//            if (range > 0) {
//                val normalized = pixels.map { pixel ->
//                    val value = Color.red(pixel)
//                    val normalizedValue = ((value - min) * 255 / range).coerceIn(0, 255)
//                    Color.rgb(normalizedValue, normalizedValue, normalizedValue)
//                }.toIntArray()
//                grayscale.setPixels(normalized, 0, STANDARD_SIZE, 0, 0, STANDARD_SIZE, STANDARD_SIZE)
//            }
//
//            grayscale
//        } catch (e: Exception) {
//            e.printStackTrace()  // Log the error for debugging
//            bitmap // Fallback to the original bitmap in case of failure
//        }
//    }
//
//    private fun extractLBPFeatures(bitmap: Bitmap): ByteArray {
//        val width = bitmap.width
//        val height = bitmap.height
//        val cellWidth = width / GRID_SIZE
//        val cellHeight = height / GRID_SIZE
//        val features = ByteArray(GRID_SIZE * GRID_SIZE)
//
//        for (gy in 0 until GRID_SIZE) {
//            for (gx in 0 until GRID_SIZE) {
//                var pattern = 0
//                val centerX = gx * cellWidth + cellWidth / 2
//                val centerY = gy * cellHeight + cellHeight / 2
//
//                val centerPixel = Color.red(bitmap.getPixel(centerX, centerY))
//
//                // Compute LBP using 8 neighbors
//                for (i in 0 until 8) {
//                    val angle = i * Math.PI / 4
//                    val nx = (centerX + LBP_RADIUS * cos(angle)).roundToInt().coerceIn(0, width - 1)
//                    val ny = (centerY + LBP_RADIUS * sin(angle)).roundToInt().coerceIn(0, height - 1)
//
//                    val neighborPixel = Color.red(bitmap.getPixel(nx, ny))
//                    if (neighborPixel > centerPixel) {
//                        pattern = pattern or (1 shl i)
//                    }
//                }
//
//                features[gy * GRID_SIZE + gx] = pattern.toByte()
//            }
//        }
//
//        return features
//    }
//
//    private fun extractIntensityFeatures(bitmap: Bitmap): ByteArray {
//        val width = bitmap.width
//        val height = bitmap.height
//        val cellWidth = width / GRID_SIZE
//        val cellHeight = height / GRID_SIZE
//        val features = ByteArray(GRID_SIZE * GRID_SIZE)
//
//        for (gy in 0 until GRID_SIZE) {
//            for (gx in 0 until GRID_SIZE) {
//                var sum = 0
//                var count = 0
//
//                for (y in gy * cellHeight until (gy + 1) * cellHeight) {
//                    for (x in gx * cellWidth until (gx + 1) * cellWidth) {
//                        sum += Color.red(bitmap.getPixel(x, y))
//                        count++
//                    }
//                }
//
//                features[gy * GRID_SIZE + gx] = if (count > 0) (sum / count).toByte() else 0
//            }
//        }
//
//        return features
//    }
//
//    private fun extractEdgeFeatures(bitmap: Bitmap): ByteArray {
//        val width = bitmap.width
//        val height = bitmap.height
//        val cellWidth = width / GRID_SIZE
//        val cellHeight = height / GRID_SIZE
//        val features = ByteArray(GRID_SIZE * GRID_SIZE * 2)  // Store both horizontal and vertical edges
//
//        for (gy in 0 until GRID_SIZE) {
//            for (gx in 0 until GRID_SIZE) {
//                var horizontalEdgeSum = 0
//                var verticalEdgeSum = 0
//                var count = 0
//
//                val startX = gx * cellWidth
//                val startY = gy * cellHeight
//
//                for (y in startY until startY + cellHeight - 1) {
//                    for (x in startX until startX + cellWidth - 1) {
//                        // Compute simple edge gradients
//                        val horizontalGradient = abs(
//                            Color.red(bitmap.getPixel(x + 1, y)) -
//                                    Color.red(bitmap.getPixel(x, y))
//                        )
//                        val verticalGradient = abs(
//                            Color.red(bitmap.getPixel(x, y + 1)) -
//                                    Color.red(bitmap.getPixel(x, y))
//                        )
//
//                        horizontalEdgeSum += horizontalGradient
//                        verticalEdgeSum += verticalGradient
//                        count++
//                    }
//                }
//
//                if (count > 0) {
//                    features[gy * GRID_SIZE * 2 + gx * 2] = (horizontalEdgeSum / count).toByte()
//                    features[gy * GRID_SIZE * 2 + gx * 2 + 1] = (verticalEdgeSum / count).toByte()
//                }
//            }
//        }
//
//        return features
//    }
//
//    private fun combineFeatures(
//        lbpFeatures: ByteArray,
//        intensityFeatures: ByteArray,
//        edgeFeatures: ByteArray
//    ): ByteArray {
//        val embedding = ByteArray(EMBEDDING_DIM)
//        var position = 0
//
//        // Combine features with weights
//        fun addFeatures(features: ByteArray, weight: Float) {
//            features.forEach { value ->
//                if (position < EMBEDDING_DIM) {
//                    embedding[position++] = ((value.toInt() and 0xFF) * weight).toInt().toByte()
//                }
//            }
//        }
//
//        addFeatures(lbpFeatures, 0.4f)      // LBP features get 40% weight
//        addFeatures(intensityFeatures, 0.3f) // Intensity features get 30% weight
//        addFeatures(edgeFeatures, 0.3f)      // Edge features get 30% weight
//
//        return embedding
//    }
//
//    private fun generateSimpleEmbedding(bitmap: Bitmap): ByteArray {
//        // Fallback method that's very simple but still provides some useful information
//        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
//        val embedding = ByteArray(EMBEDDING_DIM)
//        var position = 0
//
//        for (y in 0 until 16) {
//            for (x in 0 until 16) {
//                if (position < EMBEDDING_DIM) {
//                    embedding[position++] = Color.red(scaled.getPixel(x, y)).toByte()
//                }
//            }
//        }
//
//        return embedding
//    }
//
//    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
//        val buffer = imageProxy.planes[0].buffer
//        val bytes = ByteArray(buffer.remaining())
//        buffer.get(bytes)
//
//        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//
//        // Handle rotation
//        val matrix = Matrix()
//        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//        return Bitmap.createBitmap(
//            bitmap,
//            0,
//            0,
//            bitmap.width,
//            bitmap.height,
//            matrix,
//            true
//        )
//    }
//
//    fun compareFaces(embedding1: ByteArray, embedding2: ByteArray): Double {
//        var dotProduct = 0.0
//        var norm1 = 0.0
//        var norm2 = 0.0
//
//        for (i in embedding1.indices) {
//            val v1 = embedding1[i].toInt() and 0xFF
//            val v2 = embedding2[i].toInt() and 0xFF
//            dotProduct += v1 * v2
//            norm1 += v1 * v1
//            norm2 += v2 * v2
//        }
//
//        return if (norm1 > 0 && norm2 > 0) {
//            dotProduct / (sqrt(norm1) * sqrt(norm2))
//        } else {
//            0.0
//        }
//    }
//}


package utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import kotlin.math.*

object EmbeddingUtils {
    private const val EMBEDDING_DIM = 128
    private const val GRID_SIZE = 16
    private const val LBP_RADIUS = 1
    private const val STANDARD_SIZE = 160

    // Generate embedding from a bitmap
    fun generateEmbedding(bitmap: Bitmap): ByteArray {
        return try {
            // Step 1: Preprocess image
            val processedBitmap = preprocessImage(bitmap)

            // Step 2: Extract features
            val lbpFeatures = extractLBPFeatures(processedBitmap)
            val intensityFeatures = extractIntensityFeatures(processedBitmap)
            val edgeFeatures = extractEdgeFeatures(processedBitmap)

            // Step 3: Combine features
            combineFeatures(lbpFeatures, intensityFeatures, edgeFeatures)
        } catch (e: Exception) {
            e.printStackTrace()  // Log for debugging
            // Fallback: Generate simple embedding if advanced processing fails
            generateSimpleEmbedding(bitmap)
        }
    }

    // Preprocess image by scaling, gray scaling, and normalizing brightness
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, STANDARD_SIZE, STANDARD_SIZE, true)
            val grayscale = Bitmap.createBitmap(STANDARD_SIZE, STANDARD_SIZE, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(grayscale)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
            canvas.drawBitmap(scaled, 0f, 0f, paint)

            grayscale
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap // Fallback to original if preprocessing fails
        }
    }

    // Extract LBP (Local Binary Patterns) features
    private fun extractLBPFeatures(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val cellWidth = width / GRID_SIZE
        val cellHeight = height / GRID_SIZE
        val features = ByteArray(GRID_SIZE * GRID_SIZE)

        for (gy in 0 until GRID_SIZE) {
            for (gx in 0 until GRID_SIZE) {
                var pattern = 0
                val centerX = gx * cellWidth + cellWidth / 2
                val centerY = gy * cellHeight + cellHeight / 2
                val centerPixel = Color.red(bitmap.getPixel(centerX, centerY))

                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4
                    val nx = (centerX + LBP_RADIUS * cos(angle)).roundToInt().coerceIn(0, width - 1)
                    val ny = (centerY + LBP_RADIUS * sin(angle)).roundToInt().coerceIn(0, height - 1)
                    val neighborPixel = Color.red(bitmap.getPixel(nx, ny))

                    if (neighborPixel > centerPixel) {
                        pattern = pattern or (1 shl i)
                    }
                }
                features[gy * GRID_SIZE + gx] = pattern.toByte()
            }
        }
        return features
    }

    // Extract intensity features
    private fun extractIntensityFeatures(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val cellWidth = width / GRID_SIZE
        val cellHeight = height / GRID_SIZE
        val features = ByteArray(GRID_SIZE * GRID_SIZE)

        for (gy in 0 until GRID_SIZE) {
            for (gx in 0 until GRID_SIZE) {
                var sum = 0
                var count = 0

                for (y in gy * cellHeight until (gy + 1) * cellHeight) {
                    for (x in gx * cellWidth until (gx + 1) * cellWidth) {
                        sum += Color.red(bitmap.getPixel(x, y))
                        count++
                    }
                }
                features[gy * GRID_SIZE + gx] = if (count > 0) (sum / count).toByte() else 0
            }
        }
        return features
    }

    // Extract edge features
    private fun extractEdgeFeatures(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val cellWidth = width / GRID_SIZE
        val cellHeight = height / GRID_SIZE
        val features = ByteArray(GRID_SIZE * GRID_SIZE * 2)

        for (gy in 0 until GRID_SIZE) {
            for (gx in 0 until GRID_SIZE) {
                var horizontalEdgeSum = 0
                var verticalEdgeSum = 0
                var count = 0

                val startX = gx * cellWidth
                val startY = gy * cellHeight

                for (y in startY until startY + cellHeight - 1) {
                    for (x in startX until startX + cellWidth - 1) {
                        val horizontalGradient =
                            abs(Color.red(bitmap.getPixel(x + 1, y)) - Color.red(bitmap.getPixel(x, y)))
                        val verticalGradient =
                            abs(Color.red(bitmap.getPixel(x, y + 1)) - Color.red(bitmap.getPixel(x, y)))

                        horizontalEdgeSum += horizontalGradient
                        verticalEdgeSum += verticalGradient
                        count++
                    }
                }

                if (count > 0) {
                    features[gy * GRID_SIZE * 2 + gx * 2] = (horizontalEdgeSum / count).toByte()
                    features[gy * GRID_SIZE * 2 + gx * 2 + 1] = (verticalEdgeSum / count).toByte()
                }
            }
        }
        return features
    }

    // Combine features into a single embedding
    private fun combineFeatures(
        lbpFeatures: ByteArray,
        intensityFeatures: ByteArray,
        edgeFeatures: ByteArray
    ): ByteArray {
        val embedding = ByteArray(EMBEDDING_DIM)
        var position = 0

        fun addFeatures(features: ByteArray, weight: Float) {
            features.forEach { value ->
                if (position < EMBEDDING_DIM) {
                    embedding[position++] = ((value.toInt() and 0xFF) * weight).toInt().toByte()
                }
            }
        }

        addFeatures(lbpFeatures, 0.4f)
        addFeatures(intensityFeatures, 0.3f)
        addFeatures(edgeFeatures, 0.3f)

        return embedding
    }

    // Generate a simple fallback embedding
    private fun generateSimpleEmbedding(bitmap: Bitmap): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val embedding = ByteArray(EMBEDDING_DIM)
        var position = 0

        for (y in 0 until 16) {
            for (x in 0 until 16) {
                if (position < EMBEDDING_DIM) {
                    embedding[position++] = Color.red(scaled.getPixel(x, y)).toByte()
                }
            }
        }
        return embedding
    }

    // Convert ImageProxy to Bitmap
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    // Compare two face embeddings
    fun compareFaces(embedding1: ByteArray, embedding2: ByteArray): Double {
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in embedding1.indices) {
            val v1 = embedding1[i].toInt() and 0xFF
            val v2 = embedding2[i].toInt() and 0xFF
            dotProduct += v1 * v2
            norm1 += v1 * v1
            norm2 += v2 * v2
        }

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else {
            0.0
        }
    }
}
