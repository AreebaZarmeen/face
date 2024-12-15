package com.example.face

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Size
import android.view.View

class OverlayView(context: Context) : View(context) {
    private var faceRect: Rect? = null
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    fun setPreviewSize(size: Size) {
        previewWidth = size.width
        previewHeight = size.height
    }

    fun setFaceRect(rect: Rect?) {
        faceRect = rect?.let { originalRect ->
            // Scale the rect to match the preview view size
            val scaleX = width.toFloat() / previewWidth
            val scaleY = height.toFloat() / previewHeight

            Rect(
                (originalRect.left * scaleX).toInt(),
                (originalRect.top * scaleY).toInt(),
                (originalRect.right * scaleX).toInt(),
                (originalRect.bottom * scaleY).toInt()
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faceRect?.let { rect ->
            // Draw the main rectangle
            canvas.drawRect(rect, paint)

            // Draw corner indicators
            val cornerLength = 20f

            // Top-left corner
            canvas.drawLine(rect.left.toFloat(), rect.top.toFloat(), rect.left + cornerLength, rect.top.toFloat(), paint)
            canvas.drawLine(rect.left.toFloat(), rect.top.toFloat(), rect.left.toFloat(), rect.top + cornerLength, paint)

            // Top-right corner
            canvas.drawLine(rect.right.toFloat(), rect.top.toFloat(), rect.right - cornerLength, rect.top.toFloat(), paint)
            canvas.drawLine(rect.right.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.top + cornerLength, paint)

            // Bottom-left corner
            canvas.drawLine(rect.left.toFloat(), rect.bottom.toFloat(), rect.left + cornerLength, rect.bottom.toFloat(), paint)
            canvas.drawLine(rect.left.toFloat(), rect.bottom.toFloat(), rect.left.toFloat(), rect.bottom - cornerLength, paint)

            // Bottom-right corner
            canvas.drawLine(rect.right.toFloat(), rect.bottom.toFloat(), rect.right - cornerLength, rect.bottom.toFloat(), paint)
            canvas.drawLine(rect.right.toFloat(), rect.bottom.toFloat(), rect.right.toFloat(), rect.bottom - cornerLength, paint)
        }
    }
}