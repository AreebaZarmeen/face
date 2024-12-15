package database

import android.provider.BaseColumns

object FaceContract {
    object FaceEntry {
        const val TABLE_NAME = "faces"
        const val COLUMN_ID = "_id"  // This is the default primary key column
        const val COLUMN_NAME = "name"
        const val COLUMN_IMAGE_PATH = "image_path"
        const val COLUMN_EMBEDDING = "embedding"
    }
}
