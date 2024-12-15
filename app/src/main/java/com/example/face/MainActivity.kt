package com.example.face

import adapters.FaceAdapter
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import database.FaceDatabase
import database.face
import utils.EmbeddingUtils.generateEmbedding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var db: FaceDatabase
    private lateinit var adapter: FaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)
        android.os.StrictMode.setThreadPolicy(android.os.StrictMode.ThreadPolicy.Builder().permitAll().build())

        db = FaceDatabase(this)
        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = FaceAdapter(db.getAllFaces()) { face ->
            // Handle delete
            AlertDialog.Builder(this)
                .setTitle("Delete Face")
                .setMessage("Are you sure you want to delete ${face.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    db.deleteFace(face)
                    updateFacesList()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<RecyclerView>(R.id.rvFaces).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            selectImageFromGallery()
        }

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            captureImage()
        }

        findViewById<Button>(R.id.btnRecognize).setOnClickListener {
            startActivity(Intent(this, RecognitionActivity::class.java))
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val bitmap = when (requestCode) {
                REQUEST_GALLERY -> {
                    val uri = data?.data ?: return
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                REQUEST_CAMERA -> {
                    data?.extras?.get("data") as? Bitmap
                }
                else -> null
            } ?: return

            showAddFaceDialog(bitmap)
        }
    }

    private fun showAddFaceDialog(bitmap: Bitmap) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_face, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.dialogImageView)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)

        imageView.setImageBitmap(bitmap)

        AlertDialog.Builder(this)
            .setTitle("Add New Face")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    // Save image to internal storage
                    val imagePath = saveBitmapToFile(bitmap)
                    // Generate face embedding
                    val embedding = generateEmbedding(bitmap)
                    // Save to database
                    db.addFace(face(name = name, imagePath = imagePath, embedding = embedding))
                    updateFacesList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String {
        val filename = "face_${System.currentTimeMillis()}.jpg"
        val file = File(getExternalFilesDir(null), filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        return file.absolutePath
    }

    private fun updateFacesList() {
        adapter.updateData(db.getAllFaces())
    }

    companion object {
        private const val REQUEST_GALLERY = 1001
        private const val REQUEST_CAMERA = 1002
    }
}