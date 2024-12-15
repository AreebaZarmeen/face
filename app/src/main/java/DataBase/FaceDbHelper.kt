//package database
//
//import android.content.Context
//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteOpenHelper
//import android.provider.BaseColumns
//
//class FaceDbHelper(context: Context) : SQLiteOpenHelper(
//    context,
//    DATABASE_NAME,
//    null,
//    DATABASE_VERSION
//) {
//    override fun onCreate(db: SQLiteDatabase) {
//        db.execSQL("""
//            CREATE TABLE ${FaceContract.FaceEntry.TABLE_NAME} (
//                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
//                ${FaceContract.FaceEntry.COLUMN_NAME} TEXT NOT NULL,
//                ${FaceContract.FaceEntry.COLUMN_IMAGE_PATH} TEXT NOT NULL,
//                ${FaceContract.FaceEntry.COLUMN_EMBEDDING} BLOB NOT NULL
//            )
//        """.trimIndent())
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        // For simplicity, just drop and recreate the table
//        db.execSQL("DROP TABLE IF EXISTS ${FaceContract.FaceEntry.TABLE_NAME}")
//        onCreate(db)
//    }
//
//    companion object {
//        const val DATABASE_NAME = "FaceRecognition.db"
//        const val DATABASE_VERSION = 1
//    }
//}

package database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class FaceDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        // Create the faces table
        db.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < newVersion) {
            // Handle database upgrade, for example adding new columns, or handling schema changes.
            // For simplicity, here we are just dropping and recreating the table, but this should
            // be more sophisticated for real apps (e.g., adding migrations).
            db.execSQL("DROP TABLE IF EXISTS ${FaceContract.FaceEntry.TABLE_NAME}")
            onCreate(db)
        }
    }

    companion object {
        const val DATABASE_NAME = "FaceRecognition.db"
        const val DATABASE_VERSION = 1

        // SQL query to create the table
        private const val CREATE_TABLE_QUERY = """
            CREATE TABLE ${FaceContract.FaceEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${FaceContract.FaceEntry.COLUMN_NAME} TEXT NOT NULL,
                ${FaceContract.FaceEntry.COLUMN_IMAGE_PATH} TEXT NOT NULL,
                ${FaceContract.FaceEntry.COLUMN_EMBEDDING} BLOB NOT NULL
            )
        """
    }
}
