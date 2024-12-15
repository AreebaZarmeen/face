package database

data class face(
    val id: Long = 0,
    val name: String,
    val imagePath: String,
    val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as face
        return id == other.id &&
                name == other.name &&
                imagePath == other.imagePath &&
                embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
