/*

package database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FaceDatabase(context: Context) {
    private val dbHelper = FaceDbHelper(context)

    // Thread-safe cache for face embeddings
    private val embeddingCache = ConcurrentHashMap<Long, ByteArray>()

    // Adds a new face to the database and cache
    fun addFace(face: face): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(FaceContract.FaceEntry.COLUMN_NAME, face.name)
            put(FaceContract.FaceEntry.COLUMN_IMAGE_PATH, face.imagePath)
            put(FaceContract.FaceEntry.COLUMN_EMBEDDING, face.embedding)
        }

        // Insert the face data, returns the row ID (primary key)
        val id = db.insert(FaceContract.FaceEntry.TABLE_NAME, null, values)

        // Cache the embedding if insertion was successful
        if (id != -1L) {
            embeddingCache[id] = face.embedding
        }

        return id
    }

    // Retrieves all faces from the database and populates the cache
    fun getAllFaces(): List<face> {
        val db = dbHelper.readableDatabase
        val faces = mutableListOf<face>()

        val cursor = db.query(
            FaceContract.FaceEntry.TABLE_NAME,
            null,  // Get all columns
            null,  // No selection (select all)
            null,  // No selection args
            null,  // No group by
            null,  // No having clause
            "${FaceContract.FaceEntry.COLUMN_NAME} ASC"  // Sort by name
        )

        // Iterate through the cursor and create face objects
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_ID))
                val embedding = it.getBlob(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_EMBEDDING))

                val face = face(
                    id = id,
                    name = it.getString(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_NAME)),
                    imagePath = it.getString(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_IMAGE_PATH)),
                    embedding = embedding
                )

                faces.add(face)

                // Populate the cache
                embeddingCache[id] = embedding
            }
        }

        return faces
    }

    // Retrieves a face embedding from cache or database
    fun getFaceEmbedding(faceId: Long): ByteArray? {
        // First, check the cache
        embeddingCache[faceId]?.let { return it }

        // If not in cache, try to retrieve from database
        val db = dbHelper.readableDatabase
        val projection = arrayOf(FaceContract.FaceEntry.COLUMN_EMBEDDING)
        val selection = "${FaceContract.FaceEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(faceId.toString())

        val cursor = db.query(
            FaceContract.FaceEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                val embedding = it.getBlob(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_EMBEDDING))
                // Cache the retrieved embedding
                embeddingCache[faceId] = embedding
                embedding
            } else null
        }
    }

    // Deletes a face from the database, image file, and cache
    fun deleteFace(face: face) {
        val db = dbHelper.writableDatabase

        // Delete the database entry
        db.delete(
            FaceContract.FaceEntry.TABLE_NAME,
            "${FaceContract.FaceEntry.COLUMN_ID} = ?",
            arrayOf(face.id.toString())
        )

        // Remove from cache
        embeddingCache.remove(face.id)

        // Delete the associated image file if it exists
        val imageFile = File(face.imagePath)
        if (imageFile.exists()) {
            imageFile.delete()
        }
    }

    // Clear the entire cache
    fun clearEmbeddingCache() {
        embeddingCache.clear()
    }

    fun closeDatabase() {
        try {
            // Optional: clear cache when closing database
            embeddingCache.clear()
            dbHelper.close()
            println("Database connection closed")
        } catch (e: Exception) {
            println("Error closing the database: ${e.message}")
        }
    }
}*/


package database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// Assuming this is the data class for face
//data class face(
//    val id: Long = 0,
//    val name: String,
//    val imagePath: String,
//    val embedding: ByteArray
//) {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as face
//
//        if (id != other.id) return false
//        if (name != other.name) return false
//        if (imagePath != other.imagePath) return false
//        if (!embedding.contentEquals(other.embedding)) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = id.hashCode()
//        result = 31 * result + name.hashCode()
//        result = 31 * result + imagePath.hashCode()
//        result = 31 * result + embedding.contentHashCode()
//        return result
//    }
//}

class FaceDatabase(context: Context) {
    private val dbHelper = FaceDbHelper(context)

    // Thread-safe cache for face embeddings
    private val embeddingCache = ConcurrentHashMap<Long, ByteArray>()

    // Adds a new face to the database and cache
    fun addFace(face: face): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(FaceContract.FaceEntry.COLUMN_NAME, face.name)
            put(FaceContract.FaceEntry.COLUMN_IMAGE_PATH, face.imagePath)
            put(FaceContract.FaceEntry.COLUMN_EMBEDDING, face.embedding)
        }

        // Insert the face data, returns the row ID (primary key)
        val id = db.insert(FaceContract.FaceEntry.TABLE_NAME, null, values)

        // Cache the embedding if insertion was successful
        if (id != -1L) {
            embeddingCache[id] = face.embedding
        }

        return id
    }

    // Retrieves all faces from the database and populates the cache
    fun getAllFaces(): List<face> {
        val db = dbHelper.readableDatabase
        val faces = mutableListOf<face>()

        val cursor = db.query(
            FaceContract.FaceEntry.TABLE_NAME,
            null,  // Get all columns
            null,  // No selection (select all)
            null,  // No selection args
            null,  // No group by
            null,  // No having clause
            "${FaceContract.FaceEntry.COLUMN_NAME} ASC"  // Sort by name
        )

        // Iterate through the cursor and create face objects
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_ID))
                val embedding = it.getBlob(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_EMBEDDING))

                val face = face(
                    id = id,
                    name = it.getString(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_NAME)),
                    imagePath = it.getString(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_IMAGE_PATH)),
                    embedding = embedding
                )

                faces.add(face)

                // Populate the cache
                embeddingCache[id] = embedding
            }
        }

        return faces
    }

    // Retrieves a face embedding from cache or database
    fun getFaceEmbedding(faceId: Long): ByteArray? {
        // First, check the cache
        embeddingCache[faceId]?.let { return it }

        // If not in cache, try to retrieve from database
        val db = dbHelper.readableDatabase
        val projection = arrayOf(FaceContract.FaceEntry.COLUMN_EMBEDDING)
        val selection = "${FaceContract.FaceEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(faceId.toString())

        val cursor = db.query(
            FaceContract.FaceEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                val embedding = it.getBlob(it.getColumnIndexOrThrow(FaceContract.FaceEntry.COLUMN_EMBEDDING))
                // Cache the retrieved embedding
                embeddingCache[faceId] = embedding
                embedding
            } else null
        }
    }

    // Deletes a face from the database, image file, and cache
    fun deleteFace(face: face) {
        val db = dbHelper.writableDatabase

        // Delete the database entry
        db.delete(
            FaceContract.FaceEntry.TABLE_NAME,
            "${FaceContract.FaceEntry.COLUMN_ID} = ?",
            arrayOf(face.id.toString())
        )

        // Remove from cache
        embeddingCache.remove(face.id)

        // Delete the associated image file if it exists
        val imageFile = File(face.imagePath)
        if (imageFile.exists()) {
            imageFile.delete()
        }
    }

    // Clear the entire cache
    fun clearEmbeddingCache() {
        embeddingCache.clear()
    }

    fun closeDatabase() {
        try {
            // Optional: clear cache when closing database
            embeddingCache.clear()
            dbHelper.close()
            println("Database connection closed")
        } catch (e: Exception) {
            println("Error closing the database: ${e.message}")
        }
    }

    // Method to find a matching face, taking a bitmap as input
    suspend fun findMatchingFace(inputBitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        // This is a placeholder implementation. In a real-world scenario,
        // you would:
        // 1. Extract face embedding from the input bitmap
        // 2. Compare the embedding with stored face embeddings
        // 3. Use a similarity threshold to determine a match

        // Retrieve all faces from the database
        val faces = getAllFaces()

        // If no faces in the database, return null
        if (faces.isEmpty()) return@withContext null

        // Placeholder similarity calculation (you'll want to replace with
        // a proper face recognition algorithm)
        val matchedFace = faces.firstOrNull { /* Implement face matching logic */
            // Example pseudo-code:
            // compareEmbeddings(extractEmbedding(inputBitmap), it.embedding) > SIMILARITY_THRESHOLD
            false
        }

        // Return the name of the matched face, or null if no match
        matchedFace?.name
    }
}